- Aufschreiben welche verschieden Fragen ich habe. z.b ich kenne bereits den Task. welche properties gibt es dazu.
- systemprompts? nachforschen, ob man hier so etwas wie Weichen stellen kann. Für diese Art von Fragen hier entlang, sonst da.



Remember the main goal. A user wants to ask questions about gretl and how to write gretl tasks, e.g. "I want to import INTERLIS data into a postgis database, which task can i use? Can you make me an example. my file is called fubar.xtf it needs to be download from https://example.com". The gretl copilot answers with the build.gradle file and some explanations and sources (where the info is from). 



Das Ziel meiner Anwendung auf Basis von LLM und RAG ist ein Assistent für Endbenutzer von der Software GRETL (https://gretl.app / https://github.com/sogis/gretl). GRETL ist ein Gradle-Plugin mit vielen selber geschriebenen Tasks. Die Chatanwendung soll auf nachfolgende Fragen antworten liefern:

- "Ich muss eine INTERLIS-Datei validieren. Schreibe mir einen Task. Die Datei heisst "fubar.xtf". Das Modell ist "SO_ARP_Fubar_20251022".
- "Ich muss eine CSV-Datei herunterladen und anschliessend in eine Postgres-Datenbank importieren. Welche Tasks kann ich dazu verwenden?
- "Ich muss SQL in einer PostGIS-Datenbank ausführen. Zeige mir ein Beispiel und erkläre mir die verschiedenen Task-Properties und sage mir welche ich zwingend verwenden muss (nicht option sind).
- "Ich muss aus einer PostGIS-Datenbank eine Shapedatei exportieren. Das Schema heisst "agi_foo" und die Tabelle "boflaeche". Welche Properties benötige ich noch zwingend. Schreibe mir so gut es geht den Task.
- "Zeige mir ein Beispiel-Task von IliValidator und lieste mir sämtliche Properties auf".

Es sind einerseits Fragen welche Tasks mit welchen Properties es überhaupt gibt und andererseits soll der Assistent auch Tasks selber schreiben können. Das Wissen über sämtliche eigenen Tasks wurde in eine Datenbank importiert und Vektoren gebildet für eine Ähnlichkeitssuche (mit text-embedding-3-large). Das Datenbankschema ist in der Datei ./initdb/01_init.sql ersichtlich. 

Für eine hybride (BM25 + vector) Suche hast du mir zwei Varianten gemacht.

hybrid over doc_chunks:

WITH params AS (
  SELECT
    $1::vector(1536)      AS q_emb,
    $2::text              AS q_text,
    COALESCE($3, 0.6)::float8 AS alpha
),
-- 1) Vector candidates (higher is better after 1 - distance)
vec AS (
  SELECT
    dc.id,
    1.0 - (dc.embedding <=> (SELECT q_emb FROM params)) AS v_score
  FROM doc_chunks dc
  ORDER BY dc.embedding <=> (SELECT q_emb FROM params)
  LIMIT 100
),
-- 2) BM25 candidates
bm AS (
  SELECT
    dc.id,
    ts_rank_cd(
      to_tsvector('simple', dc.content_text),
      plainto_tsquery('simple', (SELECT q_text FROM params))
    ) AS b_score
  FROM doc_chunks dc
  WHERE to_tsvector('simple', dc.content_text)
        @@ plainto_tsquery('simple', (SELECT q_text FROM params))
  ORDER BY b_score DESC
  LIMIT 100
),
-- 3) Candidate id set
cand AS (
  SELECT id FROM vec
  UNION
  SELECT id FROM bm
),
-- 4) Join back scores (LEFT JOIN; missing scores become NULL)
scored AS (
  SELECT
    c.id,
    v.v_score,
    b.b_score
  FROM cand c
  LEFT JOIN vec v USING (id)
  LEFT JOIN bm  b USING (id)
),
-- 5) Normalize both scores into [0,1] across the candidate set
normed AS (
  SELECT
    s.id,
    CASE
      WHEN max(v_score) OVER () = min(v_score) OVER () OR v_score IS NULL
        THEN 0.0
      ELSE (v_score - min(v_score) OVER ()) / NULLIF(max(v_score) OVER () - min(v_score) OVER (), 0)
    END AS v_norm,
    CASE
      WHEN max(b_score) OVER () = min(b_score) OVER () OR b_score IS NULL
        THEN 0.0
      ELSE (b_score - min(b_score) OVER ()) / NULLIF(max(b_score) OVER () - min(b_score) OVER (), 0)
    END AS b_norm
  FROM scored s
),
-- 6) Hybrid score
ranked AS (
  SELECT
    n.id,
    n.v_norm,
    n.b_norm,
    ( (SELECT alpha FROM params) * n.b_norm
    + (1.0 - (SELECT alpha FROM params)) * n.v_norm ) AS hybrid_score
  FROM normed n
)
SELECT
  dc.id,
  dc.task_name,
  dc.heading,
  dc.url,
  dc.content_text,
  r.v_norm   AS vector_norm,
  r.b_norm   AS bm25_norm,
  r.hybrid_score
FROM ranked r
JOIN doc_chunks dc ON dc.id = r.id
ORDER BY r.hybrid_score DESC
LIMIT 20;

search across doc_chunks and task_examples:

WITH docs AS (
  SELECT id, embedding, content_text, task_name, heading, url FROM doc_chunks
  UNION ALL
  SELECT id + 1000000000 AS id, embedding, code_md AS content_text, task_name, title AS heading, NULL::text AS url
  FROM task_examples
),
params AS (...same as above...),
vec AS (
  SELECT d.id, 1.0 - (d.embedding <=> (SELECT q_emb FROM params)) AS v_score
  FROM docs d
  ORDER BY d.embedding <=> (SELECT q_emb FROM params)
  LIMIT 100
),
bm AS (
  SELECT d.id,
         ts_rank_cd(to_tsvector('simple', d.content_text),
                    plainto_tsquery('simple', (SELECT q_text FROM params))) AS b_score
  FROM docs d
  WHERE to_tsvector('simple', d.content_text)
        @@ plainto_tsquery('simple', (SELECT q_text FROM params))
  ORDER BY b_score DESC
  LIMIT 100
),
cand AS (SELECT id FROM vec UNION SELECT id FROM bm),
scored AS (
  SELECT c.id, v.v_score, b.b_score
  FROM cand c
  LEFT JOIN vec v USING (id)
  LEFT JOIN bm  b USING (id)
),
normed AS (...same as above...),
ranked AS (...same as above...)
SELECT
  d.id, d.task_name, d.heading, d.url, d.content_text,
  r.v_norm, r.b_norm, r.hybrid_score
FROM ranked r
JOIN docs d ON d.id = r.id
ORDER BY r.hybrid_score DESC
LIMIT 20;

Ebenfalls existiert eine Spring Boot Anwendung für die Kommunikation mit der Datenbank und dem LLM und dem Benutzer (Chat-GUI).

Mache mir bitte Vorschläge wie du jetzt weiter Vorgehen würdest, um die obengenannten Anwenderfragen beantworten zu können. Insbesondere weil es vielleicht leicht unterschiedliche Fragestellungen sind, die auch unterschiedlich gelöst werden müssten. Mache mir auch Vorschläge über Systemprompts. Wie siehst du die Verwendung eines spezifischen GRETL-MCP-Servers. Diesen könnte ich praktisch automatisch aus der Dokumentation ableiten (ein Task == ein Tool). Mache aber auch Überlegungen wie man es lösen könnte ohne MCP-Server.

guardrails