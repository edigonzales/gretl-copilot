package ch.so.agi.gretl.copilot.retrieval;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.pgvector.PGvector;

import ch.so.agi.gretl.copilot.intent.IntentClassification;

@Component
@ConditionalOnProperty(name = "spring.ai.openai.embedding.options.model", havingValue = "text-embedding-3-large", matchIfMissing = false)
public class DatabaseRetrievalService implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRetrievalService.class);

    private static final int EMBEDDING_DIMENSIONS = 3072;

    private static final Pattern SCORE_PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?)");

    private static final String HYBRID_QUERY = """
            WITH params AS (
              SELECT
                ?::vector(3072)      AS q_emb,
                ?::text              AS q_text,
                COALESCE(?::float8, 0.6)::float8 AS alpha,
                ?::int               AS candidate_limit
            ),
            vec AS (
              SELECT
                dc.id,
                1.0 - (dc.embedding <=> (SELECT q_emb FROM params)) AS v_score
              FROM doc_chunks dc
              ORDER BY dc.embedding <=> (SELECT q_emb FROM params)
              LIMIT (SELECT candidate_limit FROM params)
            ),
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
              LIMIT (SELECT candidate_limit FROM params)
            ),
            cand AS (
              SELECT id FROM vec
              UNION
              SELECT id FROM bm
            ),
            scored AS (
              SELECT
                c.id,
                v.v_score,
                b.b_score
              FROM cand c
              LEFT JOIN vec v USING (id)
              LEFT JOIN bm  b USING (id)
            ),
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
            ranked AS (
              SELECT
                n.id,
                n.v_norm,
                n.b_norm,
                ((SELECT alpha FROM params) * n.b_norm + (1.0 - (SELECT alpha FROM params)) * n.v_norm) AS hybrid_score
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
            LIMIT (SELECT candidate_limit FROM params)
            """;

    private final JdbcClient jdbcClient;
    private final EmbeddingModel embeddingModel;
    private final ObjectProvider<ChatModel> rerankerProvider;
    private final RetrievalProperties properties;

    public DatabaseRetrievalService(JdbcClient jdbcClient, EmbeddingModel embeddingModel,
            ObjectProvider<ChatModel> rerankerProvider, RetrievalProperties properties) {
        this.jdbcClient = jdbcClient;
        this.embeddingModel = embeddingModel;
        this.rerankerProvider = rerankerProvider;
        this.properties = properties;
    }

    @Override
    public RetrievalResult retrieve(String userMessage, IntentClassification classification) {
        log.debug("Fetch documents from database");
        float[] queryVector = embedQuery(userMessage);
        
        System.out.println("****** 1.5");
        System.out.println(queryVector.length);
        
        List<DatabaseDocument> candidates = fetchCandidates(queryVector, userMessage, properties.getAlpha(),
                properties.getCandidateLimit());
        log.debug("Candidates total: {}", candidates.size());
        
        if (candidates.isEmpty()) {
            log.warn("No retrieval candidates for query: {}", userMessage);
            return new RetrievalResult(List.of());
        }

        List<RerankedDocument> reranked = rerankCandidates(userMessage, candidates, properties.getRerankTopK());
        log.debug("Reranked documents (total): {}", reranked.size());

        return new RetrievalResult(reranked.stream().limit(properties.getFinalLimit()).map(this::toRetrievedDocument).toList());
    }

    private float[] embedQuery(String userMessage) {
        try {
            Document document = new Document(userMessage);
            System.out.println("*****" + embeddingModel.toString());
            System.out.println("*****" + embeddingModel.dimensions());
            return embeddingModel.embed(document);
        } catch (Exception ex) {
            log.warn("Failed to generate embedding, falling back to zero vector", ex);
            return new float[EMBEDDING_DIMENSIONS];
        }
    }

    private List<DatabaseDocument> fetchCandidates(float[] queryVector, String queryText, double alpha, int limit) {
        try {
            return jdbcClient.sql(HYBRID_QUERY)
                    .param(new PGvector(queryVector))
                    .param(queryText)
                    .param(alpha)
                    .param(limit)
                    .query(new DatabaseDocumentRowMapper())
                    .list();
        } catch (DataAccessException ex) {
            log.error("Failed to execute retrieval query", ex);
            return List.of();
        }
    }

    private List<RerankedDocument> rerankCandidates(String userMessage, List<DatabaseDocument> candidates, int rerankTopK) {
        ChatModel reranker = rerankerProvider.getIfAvailable();
        List<DatabaseDocument> limited = candidates.stream().limit(rerankTopK).toList();
        log.debug("limited total: {}", limited.size());

        if (reranker == null) {
            log.warn("No ChatModel available for reranking; using hybrid scores");
            return limited.stream().map(doc -> new RerankedDocument(doc, doc.hybridScore())).sorted(
                    Comparator.comparingDouble(RerankedDocument::score).reversed()).toList();
        }

        List<RerankedDocument> results = new ArrayList<>();
        for (DatabaseDocument candidate : limited) {
            double score = scoreWithCrossEncoder(reranker, userMessage, candidate);
            log.debug("score: {}", score);
            log.debug(candidate.taskName);
            results.add(new RerankedDocument(candidate, score));
        }

        return results.stream().sorted(Comparator.comparingDouble(RerankedDocument::score).reversed()).toList();
    }

    private double scoreWithCrossEncoder(ChatModel reranker, String query, DatabaseDocument candidate) {
        String passage = Optional.ofNullable(candidate.contentText()).map(String::trim).orElse("");
        if (!StringUtils.hasText(passage)) {
            return candidate.hybridScore();
        }

        String promptText = "Query: " + query + "\n\nPassage:\n" + truncateForPrompt(passage);
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(
                        "You are a retrieval cross-encoder. Score how relevant the passage is for the query. "
                                + "Return only a floating point number between 0 and 1."),
                new UserMessage(promptText)));

        try {
            String content = reranker.call(prompt).getResult().getOutput().getText();
            double parsed = parseScore(content).orElse(candidate.hybridScore());
            return clamp(parsed, 0.0, 1.0);
        } catch (Exception ex) {
            log.warn("Reranker failed, falling back to hybrid score", ex);
            return candidate.hybridScore();
        }
    }

    private Optional<Double> parseScore(String response) {
        if (response == null) {
            return Optional.empty();
        }
        Matcher matcher = SCORE_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                return Optional.of(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ex) {
                log.debug("Unable to parse reranker score from {}", response, ex);
            }
        }
        return Optional.empty();
    }

    private String truncateForPrompt(String text) {
        int maxLength = 2000;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }

    private RetrievedDocument toRetrievedDocument(RerankedDocument doc) {
        DatabaseDocument candidate = doc.document();
        String snippet = buildSnippet(candidate.contentText());
        return new RetrievedDocument(categoryFromTask(candidate.taskName()), titleFromHeading(candidate.heading()), snippet,
                candidate.url(), doc.score());
    }

    private String categoryFromTask(String taskName) {
        if (!StringUtils.hasText(taskName)) {
            return "Document";
        }
        int slashIndex = taskName.indexOf('/');
        if (slashIndex > 0) {
            return taskName.substring(0, slashIndex);
        }
        return taskName;
    }

    private String titleFromHeading(String heading) {
        if (StringUtils.hasText(heading)) {
            return heading;
        }
        return "GRETL documentation";
    }

    private String buildSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\n", " ").replaceAll("\s+", " ").trim();
        int limit = 320;
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "…";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DatabaseDocument(long id, String taskName, String heading, String url, String contentText,
            double vectorNorm, double bm25Norm, double hybridScore) {
    }

    private record RerankedDocument(DatabaseDocument document, double score) {
    }

    private static class DatabaseDocumentRowMapper implements RowMapper<DatabaseDocument> {
        @Override
        public DatabaseDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DatabaseDocument(rs.getLong("id"), rs.getString("task_name"), rs.getString("heading"),
                    rs.getString("url"), rs.getString("content_text"), getDouble(rs, "vector_norm"),
                    getDouble(rs, "bm25_norm"), getDouble(rs, "hybrid_score"));
        }

        private double getDouble(ResultSet rs, String column) throws SQLException {
            double value = rs.getDouble(column);
            if (rs.wasNull()) {
                return 0.0;
            }
            return value;
        }
    }
}
