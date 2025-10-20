///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.jsoup:jsoup:1.17.2
//DEPS org.postgresql:postgresql:42.7.3
//DEPS com.squareup.okhttp3:okhttp:4.12.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//DEPS com.fasterxml.jackson.core:jackson-core:2.17.1
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.17.1

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.sql.DriverManager;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class ingest_gretl {
  // ----- Config (env with sensible defaults) --------------------------------
  static final String JDBC_URL = env("JDBC_URL", "jdbc:postgresql://localhost:5432/gretl_rag");
  static final String JDBC_USER = env("JDBC_USER", "gretl");
  static final String JDBC_PASS = env("JDBC_PASS", "gretl");
  static final String ROOT      = env("ROOT", "https://gretl.app");

  // pgvector dimension; must match your table definition
  static final int    EMB_DIM   = Integer.parseInt(env("EMB_DIM", "1536"));

  // OpenAI
  static final String OPENAI_API_KEY   = env("OPENAI_API_KEY", "");
  static final String OPENAI_BASE_URL  = env("OPENAI_BASE_URL", "https://api.openai.com");
  static final String OPENAI_EMB_MODEL = env("OPENAI_EMBED_MODEL", "text-embedding-3-small"); // 1536-d
  static final int    OPENAI_TIMEOUT_S = Integer.parseInt(env("OPENAI_TIMEOUT_S", "60"));

  // --- HARDCODED WHITELIST --------------------------------------------------
  static final List<Pattern> ALLOWLIST = List.of(
      Pattern.compile("^https://gretl\\.app/reference\\.html(#.*)?$")
  );

  // Optional: restrict to specific anchors (empty => allow all anchors)
  static final Set<String> TASK_ANCHORS = Set.of(
      // "Ili2pgImport", "Ili2pgValidate", "GpkgExport", "Curl"
  );

  // HTTP / JSON singletons
  static final OkHttpClient http = new OkHttpClient.Builder()
      .callTimeout(java.time.Duration.ofSeconds(OPENAI_TIMEOUT_S))
      .build();
  static final ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    require(!OPENAI_API_KEY.isBlank(), "OPENAI_API_KEY is required");

    boolean doReset = hasFlag(args, "--reset", "-r");

    Queue<String> q = new ArrayDeque<>();
    Set<String> seen = new HashSet<>();
    q.add(ROOT + "/reference.html");

    try (java.sql.Connection cx = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
      cx.setAutoCommit(false);

      if (doReset) {
        resetDatabase(cx);
        cx.commit();
        System.out.println("✅ Database content wiped (fresh start).");
      }

      while (!q.isEmpty()) {
        String url = q.poll();
        if (seen.contains(url) || !isAllowed(url)) continue;
        seen.add(url);

        try {
          org.jsoup.nodes.Document doc = Jsoup.connect(url).timeout(30_000).get();
          Element main = Optional.ofNullable(doc.selectFirst("main, .md-content, .content, article"))
                                 .orElse(doc.body());
          String title = doc.title();
          String pageMd = main.text(); // simple archive; swap to HTML→MD if you want

          long pageId = upsertPage(cx, url, title, pageMd);

          // Sections by H2/H3
          for (Element h : main.select("h2, h3")) {
            String heading = norm(h.text());
            String anchor = h.id();
            String sectionUrl = (anchor != null && !anchor.isEmpty()) ? url + "#" + anchor : url;

            if (isReference(url) && anchor != null && !anchor.isEmpty() && !isAllowedAnchor(sectionUrl)) {
              continue;
            }

            // Collect nodes until next h2/h3
            List<Node> nodes = new ArrayList<>();
            for (Node sib = h.nextSibling(); sib != null; sib = sib.nextSibling()) {
              if (sib instanceof Element) {
                String tag = ((Element) sib).tagName();
                if ("h2".equals(tag) || "h3".equals(tag)) break;
              }
              nodes.add(sib);
            }
            String sectionHtml = nodes.stream().map(Node::outerHtml).collect(Collectors.joining());
            String sectionText = Jsoup.parse(sectionHtml).text();

            String taskName = guessTask(heading);
            insertChunk(cx, pageId, taskName, "task", sectionUrl, anchor, heading, sectionText, sectionHtml);

            // Parameter tables
            Element frag = Jsoup.parse(sectionHtml).body();
            for (Element table : frag.select("table")) {
              List<String> headers = new ArrayList<>();
              Elements thead = table.select("thead th");
              if (!thead.isEmpty()) {
                for (Element th : thead) headers.add(norm(th.text()).toLowerCase());
              } else {
                Element firstRow = table.selectFirst("tr");
                if (firstRow != null) {
                  for (Element th : firstRow.select("th,td")) headers.add(norm(th.text()).toLowerCase());
                }
              }
              Map<String,Integer> idx = new HashMap<>();
              for (int i=0;i<headers.size();i++) idx.put(headers.get(i), i);

              for (Element tr : table.select("tbody tr")) {
                Elements tds = tr.select("td");
                String prop = get(tds, idx.getOrDefault("property", 0));
                if (isBlank(prop)) continue;
                String type = get(tds, idx.getOrDefault("type", 1));
                String required = get(tds, idx.getOrDefault("required", 2));
                String def = get(tds, idx.getOrDefault("default", 3));
                String desc = get(tds, idx.getOrDefault("description", 4));

                upsertProp(cx,
                    (taskName != null ? taskName : heading),
                    prop, type, isRequired(required), def, desc, null);
              }
            }

            // Code examples
            for (Element code : frag.select("pre > code")) {
              String lang = code.className(); // e.g., "language-gradle"
              String codeMd = "```" + lang.replace("language-", "") + "\n" + code.text() + "\n```";
              insertExample(cx, (taskName != null ? taskName : heading), heading + " example", codeMd, null);
            }
          }

          // Discover links but stay in allowlist
          for (Element a : doc.select("a[href]")) {
            String href = a.attr("abs:href");
            if (isAllowed(href) && (!isReference(href) || isAllowedAnchor(href))) {
              q.add(href);
            }
          }

          cx.commit();
          System.out.println("Ingested: " + url);
        } catch (Exception e) {
          cx.rollback();
          System.err.println("ERROR " + url + ": " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }

  // ------------------------ Reset helper ------------------------------------
  static void resetDatabase(java.sql.Connection cx) throws Exception {
    // Order is irrelevant with TRUNCATE ... CASCADE, but listed for clarity.
    String sql = """
      TRUNCATE TABLE
        doc_chunks,
        task_properties,
        task_examples,
        pages
      RESTART IDENTITY CASCADE
    """;
    try (java.sql.Statement st = cx.createStatement()) {
      st.execute(sql);
    }
  }

  // ------------------------ Whitelist helpers -------------------------------
  static boolean isAllowed(String url) {
    for (Pattern p : ALLOWLIST) if (p.matcher(url).find()) return true;
    return false;
  }
  static boolean isReference(String url) { return url.startsWith("https://gretl.app/reference.html"); }
  static boolean isAllowedAnchor(String url) {
    if (TASK_ANCHORS.isEmpty()) return true;
    var m = Pattern.compile("^https://gretl\\.app/reference\\.html#([A-Za-z0-9_-]+)$").matcher(url);
    return m.find() && TASK_ANCHORS.contains(m.group(1));
  }

  // ------------------------ DB ops ------------------------------------------
  static long upsertPage(java.sql.Connection cx, String url, String title, String rawMd) throws Exception {
    String sql = """
        INSERT INTO pages(url,title,raw_md)
        VALUES (?,?,?)
        ON CONFLICT (url)
        DO UPDATE SET title=EXCLUDED.title, raw_md=EXCLUDED.raw_md, fetched_at=now()
        RETURNING id
      """;
    try (java.sql.PreparedStatement ps = cx.prepareStatement(sql)) {
      ps.setString(1, url);
      ps.setString(2, title);
      ps.setString(3, rawMd);
      java.sql.ResultSet rs = ps.executeQuery();
      rs.next();
      return rs.getLong(1);
    }
  }

  static void insertChunk(java.sql.Connection cx, long pageId, String task, String sectionType,
                          String url, String anchor, String heading, String text, String mdHtml) throws Exception {
    float[] emb = openaiEmbed(heading + "\n" + text);
    String sql = """
        INSERT INTO doc_chunks
          (page_id,task_name,section_type,url,anchor,heading,content_text,content_md,embedding)
        VALUES
          (?,?,?,?,?,?,?,?, ?::vector)
      """;
    try (java.sql.PreparedStatement ps = cx.prepareStatement(sql)) {
      ps.setLong(1, pageId);
      ps.setString(2, task);
      ps.setString(3, sectionType);
      ps.setString(4, url);
      ps.setString(5, anchor);
      ps.setString(6, heading);
      ps.setString(7, text);
      ps.setString(8, mdHtml);
      ps.setString(9, vectorLiteral(emb));
      ps.executeUpdate();
    }
  }

  static void upsertProp(java.sql.Connection cx, String task, String name, String type,
                         boolean req, String def, String desc, String[] enums) throws Exception {
    String sql = """
        INSERT INTO task_properties(task_name,property_name,type,required,default_value,description,enum_values)
        VALUES (?,?,?,?,?,?,?)
        ON CONFLICT DO NOTHING
      """;
    try (java.sql.PreparedStatement ps = cx.prepareStatement(sql)) {
      ps.setString(1, task);
      ps.setString(2, name);
      ps.setString(3, type);
      ps.setBoolean(4, req);
      ps.setString(5, def);
      ps.setString(6, desc);
      if (enums == null) {
        ps.setArray(7, null);
      } else {
        ps.setArray(7, cx.createArrayOf("text", enums));
      }
      ps.executeUpdate();
    }
  }

  static void insertExample(java.sql.Connection cx, String task, String title, String codeMd, String expl) throws Exception {
    float[] emb = openaiEmbed(title + "\n" + codeMd + (expl == null ? "" : "\n" + expl));
    String sql = """
        INSERT INTO task_examples(task_name,title,code_md,explanation,embedding)
        VALUES (?,?,?,?, ?::vector)
      """;
    try (java.sql.PreparedStatement ps = cx.prepareStatement(sql)) {
      ps.setString(1, task);
      ps.setString(2, title);
      ps.setString(3, codeMd);
      ps.setString(4, expl);
      ps.setString(5, vectorLiteral(emb));
      ps.executeUpdate();
    }
  }

  // ------------------------ OpenAI Embeddings -------------------------------
  static float[] openaiEmbed(String text) throws Exception {
    ObjectNode req = mapper.createObjectNode();
    req.put("model", OPENAI_EMB_MODEL);
    req.put("input", text.replaceAll("\\s+", " ").trim());

    Request request = new Request.Builder()
        .url(OPENAI_BASE_URL + "/v1/embeddings")
        .header("Authorization", "Bearer " + OPENAI_API_KEY)
        .header("Content-Type", "application/json")
        .post(RequestBody.create(req.toString(), MediaType.parse("application/json")))
        .build();

    try (Response resp = http.newCall(request).execute()) {
      if (!resp.isSuccessful()) {
        String body = resp.body() != null ? resp.body().string() : "";
        throw new RuntimeException("OpenAI error " + resp.code() + ": " + body);
      }
      JsonNode root = mapper.readTree(resp.body().byteStream());
      JsonNode arr = root.path("data").get(0).path("embedding");
      if (!arr.isArray()) throw new RuntimeException("Unexpected embedding response");
      int n = arr.size();
      float[] out = new float[EMB_DIM];
      for (int i = 0; i < EMB_DIM; i++) {
        out[i] = (i < n) ? (float) arr.get(i).asDouble() : 0.0f; // truncate/pad
      }
      return out;
    }
  }

  // ------------------------ Utils -------------------------------------------
  static boolean hasFlag(String[] args, String... flags) {
    var set = new HashSet<>(Arrays.asList(args));
    for (String f : flags) if (set.contains(f)) return true;
    return false;
  }

  static String env(String k, String d) { String v = System.getenv(k); return (v == null || v.isEmpty()) ? d : v; }
  static void require(boolean cond, String msg) { if (!cond) throw new IllegalArgumentException(msg); }
  static String norm(String s) { return s == null ? "" : s.replaceAll("\\s+", " ").trim(); }
  static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  static boolean isRequired(String s) { if (s==null) return false; String x=s.toLowerCase(Locale.ROOT); return x.contains("yes")||x.contains("true")||x.contains("required"); }
  static String get(Elements tds, int idx){ return idx<tds.size()? tds.get(idx).text().trim(): null; }
  static String guessTask(String heading) { String first = heading.split(" ")[0]; return first.matches("[A-Z][A-Za-z0-9]*") ? first : null; }

  // pgvector helper: build "[0.1,0.2,...]" without Arrays.stream on float[]
  static String vectorLiteral(float[] v) {
    StringBuilder sb = new StringBuilder(16 * v.length);
    sb.append('[');
    for (int i = 0; i < v.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(String.format(Locale.ROOT, "%.6f", v[i]));
    }
    sb.append(']');
    return sb.toString();
  }
}
