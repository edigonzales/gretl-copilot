package ch.so.agi.gretl.copilot.intent;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.pgvector.PGvector;

@Component
@ConditionalOnBean(EmbeddingModel.class)
public class DatabaseIntentClassifier implements IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIntentClassifier.class);

    private static final String INTENT_QUERY = """
            SELECT
                task_name,
                title,
                explanation,
                1 - (embedding <=> ?) AS similarity
            FROM rag.task_examples
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> ?
            LIMIT ?
            """;

    private final JdbcClient jdbcClient;
    private final EmbeddingModel embeddingModel;
    private final IntentClassifierProperties properties;
    public DatabaseIntentClassifier(JdbcClient jdbcClient, EmbeddingModel embeddingModel,
            IntentClassifierProperties properties) {
        this.jdbcClient = jdbcClient;
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    @Override
    public IntentClassification classify(String userMessage) {
        if (log.isDebugEnabled()) {
            String preview = userMessage == null ? "" : userMessage.substring(0, Math.min(userMessage.length(), 120));
            log.debug("classify() invoked with message preview: '{}'", preview);
        }
        if (!StringUtils.hasText(userMessage)) {
            return new IntentClassification(properties.getFallbackLabel(), 0.0,
                    "Leere Benutzereingabe – kein Intent bestimmbar.");
        }

        float[] embedding = embed(userMessage);
        if (embedding.length == 0) {
            return fallback("Embedding konnte nicht erzeugt werden.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Embedding created with {} dimensions", embedding.length);
        }

        List<IntentCandidate> candidates = fetchCandidates(embedding, properties.getTopK());
        if (candidates.isEmpty()) {
            return fallback("Keine passenden Intent-Beispiele in der Datenbank gefunden.");
        }

        IntentCandidate best = candidates.get(0);
        double confidence = computeConfidence(candidates);
        String rationale = buildRationale(best, candidates);

        if (confidence < properties.getMinConfidence()) {
            if (log.isDebugEnabled()) {
                log.debug("Confidence {} below threshold {}; using fallback", confidence, properties.getMinConfidence());
            }
            return new IntentClassification(properties.getFallbackLabel(), properties.getFallbackConfidence(), rationale);
        }

        IntentClassification classification = new IntentClassification(toLabel(best.taskName()), confidence, rationale);
        if (log.isDebugEnabled()) {
            log.debug("Returning classification {} with confidence {}", classification.label(), classification.confidence());
        }
        return classification;
    }

    private IntentClassification fallback(String reason) {
        if (log.isDebugEnabled()) {
            log.debug("fallback() returning fallback label due to: {}", reason);
        }
        return new IntentClassification(properties.getFallbackLabel(), clamp(properties.getFallbackConfidence()), reason);
    }

    private float[] embed(String text) {
        if (log.isDebugEnabled()) {
            log.debug("embed() generating embedding via {}", embeddingModel.getClass().getSimpleName());
        }
        try {
            Document document = new Document(text);
            return embeddingModel.embed(document);
        } catch (Exception ex) {
            log.error("Failed to create intent embedding", ex);
            return new float[0];
        }
    }

    private List<IntentCandidate> fetchCandidates(float[] embedding, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        if (log.isDebugEnabled()) {
            log.debug("fetchCandidates() querying rag.task_examples with topK={}", limit);
        }
        try {
            List<IntentCandidate> results = jdbcClient.sql(INTENT_QUERY)
                    .param(new PGvector(embedding))
                    .param(new PGvector(embedding))
                    .param(limit)
                    .query(intentRowMapper())
                    .list();
            List<IntentCandidate> sorted = results.stream()
                    .sorted(Comparator.comparingDouble(IntentCandidate::similarity).reversed())
                    .toList();
            if (log.isDebugEnabled()) {
                log.debug("fetchCandidates() received {} candidates", sorted.size());
            }
            return sorted;
        } catch (DataAccessException ex) {
            log.error("Failed to run intent classification query", ex);
            return List.of();
        }
    }

    private RowMapper<IntentCandidate> intentRowMapper() {
        return (rs, rowNum) -> new IntentCandidate(rs.getString("task_name"), rs.getString("title"),
                rs.getString("explanation"), normalizeScore(rs.getDouble("similarity")));
    }

    private double normalizeScore(double rawScore) {
        if (Double.isNaN(rawScore) || rawScore < 0.0) {
            return 0.0;
        }
        return clamp(rawScore);
    }

    private double computeConfidence(List<IntentCandidate> candidates) {
        if (candidates.isEmpty()) {
            return 0.0;
        }
        IntentCandidate best = candidates.get(0);
        double topScore = best.similarity();
        double secondScore = candidates.size() > 1 ? candidates.get(1).similarity() : 0.0;
        double diversityBoost = Math.max(0.0, topScore - secondScore);
        double confidence = (0.7 * topScore) + (0.3 * diversityBoost);
        return clamp(confidence);
    }

    private String buildRationale(IntentCandidate best, List<IntentCandidate> candidates) {
        List<String> parts = new ArrayList<>();
        parts.add("Beste Übereinstimmung: " + best.title() + " (" + formatPercent(best.similarity()) + ")");

        if (StringUtils.hasText(best.explanation())) {
            parts.add(best.explanation().strip());
        }

        if (candidates.size() > 1) {
            IntentCandidate second = candidates.get(1);
            parts.add("Zweitplatzierter Treffer: " + second.title() + " (" + formatPercent(second.similarity()) + ")");
        }

        return String.join(". ", parts);
    }

    private String formatPercent(double value) {
        return NumberFormat.getPercentInstance(Locale.GERMAN).format(clamp(value));
    }

    private String toLabel(String taskName) {
        if (!StringUtils.hasText(taskName)) {
            return properties.getFallbackLabel();
        }
        return "task." + taskName.trim().toLowerCase(Locale.ROOT);
    }

    private double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record IntentCandidate(String taskName, String title, String explanation, double similarity) {
    }
}
