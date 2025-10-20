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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
  static final String JDBC_URL = env("JDBC_URL", "jdbc:postgresql://localhost:54323/gretl_rag");
  static final String JDBC_USER = env("JDBC_USER", "gretl");
  static final String JDBC_PASS = env("JDBC_PASS", "gretl");
  static final String ROOT      = env("ROOT", "https://gretl.app");
  static final String USER_AGENT = env("USER_AGENT",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

  // pgvector dimension; must match your table definition
  static final int    EMB_DIM   = Integer.parseInt(env("EMB_DIM", "1536"));

  // OpenAI
  static final String OPENAI_API_KEY   = env("OPENAI_API_KEY", "");
  static final String OPENAI_BASE_URL  = env("OPENAI_BASE_URL", "https://api.openai.com");
  static final String OPENAI_EMB_MODEL = env("OPENAI_EMBED_MODEL", "text-embedding-3-small"); // 1536-d
  static final int    OPENAI_TIMEOUT_S = Integer.parseInt(env("OPENAI_TIMEOUT_S", "60"));

  // Feature toggles (env defaults, overridable via CLI flags)
  static boolean dbEnabled     = Boolean.parseBoolean(env("ENABLE_DB", "true"));
  static boolean openaiEnabled = Boolean.parseBoolean(env("ENABLE_OPENAI", "true"));

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
    String inputPath = option(args, "--input", "--file");
    String startUrlOverride = option(args, "--start-url", "--url");

    boolean disableDbFlag = hasFlag(args, "--no-db");
    boolean forceDbFlag = hasFlag(args, "--db");
    if (disableDbFlag) dbEnabled = false;
    if (forceDbFlag) dbEnabled = true;

    boolean disableOpenaiFlag = hasFlag(args, "--no-openai", "--no-embeddings");
    boolean forceOpenaiFlag = hasFlag(args, "--openai", "--embeddings");
    if (disableOpenaiFlag) openaiEnabled = false;
    if (forceOpenaiFlag) openaiEnabled = true;

    boolean apiKeyPresent = !OPENAI_API_KEY.isBlank();
    boolean openaiNoticePrinted = false;
    if (openaiEnabled && !apiKeyPresent) {
      if (forceOpenaiFlag) {
        require(false, "OPENAI_API_KEY is required when embeddings are enabled");
      } else {
        openaiEnabled = false;
        openaiNoticePrinted = true;
        System.out.println("⚠️  OPENAI_API_KEY not set – disabling embeddings (vectors will be zeroed).");
      }
    }

    if (openaiEnabled) {
      require(apiKeyPresent, "OPENAI_API_KEY is required when embeddings are enabled");
    } else if (!openaiNoticePrinted) {
      System.out.println("⚠️  Running without OpenAI embeddings (vectors will be zeroed).");
    }

    if (!dbEnabled) {
      System.out.println("⚠️  Database writes disabled – results will be printed only.");
    }

    boolean doReset = hasFlag(args, "--reset", "-r");

    try (java.sql.Connection cx = dbEnabled ? DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS) : null) {
      if (cx != null) {
        try (java.sql.Statement st = cx.createStatement()) {
          st.execute("SET search_path TO rag");
        }
        cx.setAutoCommit(false);
      }

      if (doReset) {
        if (cx != null) {
          resetDatabase(cx);
          cx.commit();
          System.out.println("✅ Database content wiped (fresh start).");
        } else {
          System.out.println("ℹ️  Reset requested but database is disabled – skipping.");
        }
      }

      if (inputPath != null) {
        File inputFile = new File(inputPath);
        require(inputFile.exists(), "Input file not found: " + inputPath);
        String baseUrl = inputFile.toURI().toString();
        try {
          org.jsoup.nodes.Document doc = parseLocalFile(inputFile, baseUrl);
          processDocument(doc, baseUrl, cx);
          if (cx != null) cx.commit();
          System.out.println("Ingested: " + baseUrl);
        } catch (Exception e) {
          if (cx != null) cx.rollback();
          throw e;
        }
      } else {
        Queue<String> q = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        String startUrl = determineStartUrl(startUrlOverride);
        q.add(startUrl);

        while (!q.isEmpty()) {
          String url = q.poll();
          if (seen.contains(url) || !isAllowed(url)) continue;
          seen.add(url);

          try {
            org.jsoup.nodes.Document doc = fetchDocument(url);
            List<String> discovered = processDocument(doc, url, cx);
            for (String href : discovered) {
              if (!seen.contains(href)) {
                q.add(href);
              }
            }

            if (cx != null) cx.commit();
            System.out.println("Ingested: " + url);
          } catch (Exception e) {
            if (cx != null) cx.rollback();
            System.err.println("ERROR " + url + ": " + e.getMessage());
            e.printStackTrace();
          }
        }
      }
    }
  }

  static List<String> processDocument(org.jsoup.nodes.Document doc, String url, java.sql.Connection cx) throws Exception {
    Element main = Optional.ofNullable(doc.selectFirst("section#tasks.level2, div#tasks.level2, section#tasks, div#tasks"))
                           .orElseGet(() -> Optional.ofNullable(doc.selectFirst("main, .md-content, .content, article"))
                                                   .orElse(doc.body()));
    String title = doc.title();
    String pageMd = main.text();

    long pageId = upsertPage(cx, url, title, pageMd);

    for (Element h : main.select("h2, h3")) {
      String heading = norm(h.text());
      String anchor = h.attr("data-anchor-id");
      String baseUrl = url.contains("#") ? url.substring(0, url.indexOf('#')) : url;
      String sectionUrl = (anchor != null && !anchor.isEmpty()) ? baseUrl + "#" + anchor : url;

      if ("h2".equals(h.tagName()) && (anchor == null || anchor.isEmpty()) && h.parent() == main) {
        continue;
      }

      if (isReference(url) && anchor != null && !anchor.isEmpty() && !isAllowedAnchor(sectionUrl)) {
        continue;
      }

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
      if (!dbEnabled) {
        System.out.printf(Locale.ROOT, "→ Section [%s] %s%n", heading, sectionUrl);
      }

      Element frag = Jsoup.parse(sectionHtml).body();
      for (Element table : frag.select("table")) {
        List<String> headers = new ArrayList<>();
        Elements thead = table.select("thead th");
        if (!thead.isEmpty()) {
          for (Element th : thead) headers.add(norm(th.text()).toLowerCase(Locale.ROOT));
        } else {
          Element firstRow = table.selectFirst("tr");
          if (firstRow != null) {
            for (Element th : firstRow.select("th,td")) headers.add(norm(th.text()).toLowerCase(Locale.ROOT));
          }
        }
        Map<String,Integer> idx = new HashMap<>();
        for (int i=0;i<headers.size();i++) idx.put(headers.get(i), i);

        int propIdx = firstIndex(headers, "property", "parameter", "name", "task", "feld");
        int typeIdx = firstIndex(headers, "type", "datatype", "data type", "datentyp", "typ");
        int defaultIdx = firstIndex(headers, "default", "standard", "standardwert", "default value", "vorgabe");
        int descIdx = firstIndex(headers, "description", "beschreibung", "details", "erklärung", "erlaeuterung");
        int requiredIdx = firstIndex(headers, "required", "pflicht", "mandatory", "optional");
        boolean columnIsOptional = requiredIdx >= 0 && headers.get(requiredIdx).contains("optional");
        if (propIdx < 0) propIdx = idx.getOrDefault("property", 0);
        if (typeIdx < 0) typeIdx = idx.getOrDefault("type", 1);
        if (defaultIdx < 0) defaultIdx = idx.getOrDefault("default", headers.size() > 3 ? 3 : -1);
        if (descIdx < 0) descIdx = idx.getOrDefault("description", headers.size() > 2 ? 2 : -1);
        if (requiredIdx < 0) requiredIdx = idx.getOrDefault("required", headers.size() > 3 ? 3 : -1);

        Elements rows = table.select("tbody tr");
        if (rows.isEmpty()) rows = table.select("tr");
        for (Element tr : rows) {
          Elements tds = tr.select("td");
          String prop = get(tds, propIdx >= 0 ? propIdx : 0);
          if (isBlank(prop)) continue;
          String type = get(tds, typeIdx);
          String required = get(tds, requiredIdx);
          String def = get(tds, defaultIdx);
          String desc = get(tds, descIdx);

          boolean isReq = parseRequired(required, columnIsOptional);
          upsertProp(cx,
              (taskName != null ? taskName : heading),
              prop, type, isReq, def, desc, null);
          if (!dbEnabled) {
            System.out.printf(Locale.ROOT, "   · %s (%s) required=%s%n", prop, type, isReq);
          }
        }
      }

      for (Element code : frag.select("pre > code")) {
        String lang = code.className();
        String codeMd = "```" + lang.replace("language-", "") + "\n" + code.text() + "\n```";
        insertExample(cx, (taskName != null ? taskName : heading), heading + " example", codeMd, null);
      }
    }

    String baseUrl = stripFragment(url);
    Set<String> unique = new LinkedHashSet<>();
    for (Element a : doc.select("a[href]")) {
      String href = a.attr("abs:href");
      if (isBlank(href)) continue;

      String normalized = stripFragment(href);
      if (!isAllowed(normalized)) continue;
      if (normalized.equals(baseUrl)) continue;

      // Skip looping over task anchors on the reference page – sections are
      // already processed when the parent document is ingested.
      if (isReference(baseUrl) && href.startsWith(baseUrl + "#")) continue;

      unique.add(normalized);
    }
    return new ArrayList<>(unique);
  }

  static String stripFragment(String url) {
    int idx = url.indexOf('#');
    return idx >= 0 ? url.substring(0, idx) : url;
  }

  static org.jsoup.nodes.Document fetchDocument(String location) throws Exception {
    if (location.startsWith("file:")) {
      File file = new File(java.net.URI.create(location));
      return parseLocalFile(file, location);
    }

    if (location.startsWith("http://") || location.startsWith("https://")) {
      Request request = new Request.Builder()
          .url(location)
          .header("User-Agent", USER_AGENT)
          .header("Accept", "text/html,application/xhtml+xml")
          .build();
      try (Response resp = http.newCall(request).execute()) {
        if (!resp.isSuccessful() || resp.body() == null) {
          String body = resp.body() != null ? resp.body().string() : "";
          throw new RuntimeException("Failed to download " + location + ": " + resp.code() + " " + body);
        }

        byte[] bytes = resp.body().bytes();
        Path temp = Files.createTempFile("gretl-ref-", ".html");
        try (FileOutputStream fos = new FileOutputStream(temp.toFile())) {
          fos.write(bytes);
        }
        temp.toFile().deleteOnExit();
        return parseLocalFile(temp.toFile(), location);
      }
    }

    File maybeFile = new File(location);
    if (maybeFile.exists()) {
      return parseLocalFile(maybeFile, maybeFile.toURI().toString());
    }

    throw new IllegalArgumentException("Unsupported location: " + location);
  }

  static org.jsoup.nodes.Document parseLocalFile(File inputFile, String baseUrl) throws Exception {
    return Jsoup.parse(inputFile, "UTF-8", baseUrl);
  }

  static String determineStartUrl(String override) {
    if (override == null || override.isBlank()) {
      return ROOT + "/reference.html";
    }

    File local = new File(override);
    if (local.exists()) {
      return local.toURI().toString();
    }

    if (override.startsWith("file:")) {
      return override;
    }

    if (!override.contains("://")) {
      if (override.startsWith("/")) {
        return ROOT + override;
      }
      return ROOT + "/" + override;
    }

    return override;
  }

  // ------------------------ Reset helper ------------------------------------
  static void resetDatabase(java.sql.Connection cx) throws Exception {
    if (cx == null) return;
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
    if (url.startsWith("file:")) return true;
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
    if (cx == null) return -1L;
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
    if (cx == null) return;
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
    if (cx == null) return;
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
    if (cx == null) return;
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
    if (!openaiEnabled) {
      return new float[EMB_DIM];
    }
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

  static String option(String[] args, String... keys) {
    for (int i = 0; i < args.length; i++) {
      for (String key : keys) {
        if (args[i].equals(key)) {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for option " + key);
          }
          return args[i + 1];
        }
      }
    }
    return null;
  }

  static String env(String k, String d) { String v = System.getenv(k); return (v == null || v.isEmpty()) ? d : v; }
  static void require(boolean cond, String msg) { if (!cond) throw new IllegalArgumentException(msg); }
  static String norm(String s) { return s == null ? "" : s.replaceAll("\\s+", " ").trim(); }
  static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  static String get(Elements tds, int idx){
    if (idx < 0 || idx >= tds.size()) return null;
    return norm(tds.get(idx).text());
  }
  static String guessTask(String heading) {
    String[] parts = heading.split(" ");
    if (parts.length == 0) return null;
    String first = parts[0];
    return first.matches("[A-Z][A-Za-z0-9]*") ? first : null;
  }
  static int firstIndex(List<String> headers, String... options) {
    for (int i = 0; i < headers.size(); i++) {
      String h = headers.get(i);
      for (String opt : options) {
        if (h.contains(opt)) return i;
      }
    }
    return -1;
  }
  static boolean parseRequired(String value, boolean columnIsOptional) {
    if (value == null) return false;
    String v = value.toLowerCase(Locale.ROOT);
    boolean yes = containsAny(v, List.of("yes", "true", "required", "ja", "oui", "si", "sim"));
    boolean no = containsAny(v, List.of("no", "false", "optional", "nein", "non", "na", "não", "nao"));
    if (columnIsOptional) {
      if (yes) return false;
      if (no) return true;
      return false;
    } else {
      if (yes) return true;
      if (no) return false;
      return false;
    }
  }
  static boolean containsAny(String haystack, List<String> needles) {
    for (String n : needles) if (haystack.contains(n)) return true;
    return false;
  }

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
