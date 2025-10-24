package ch.so.agi.gretl.copilot.intent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(name = "databaseIntentClassifier",
        value = org.springframework.ai.embedding.EmbeddingModel.class)
public class MockIntentClassifier implements IntentClassifier {
    @Override
    public IntentClassification classify(String userMessage) {
        return new IntentClassification("task.ili2pgimport", 0.87,
                "Matched keywords for INTERLIS import and PostGIS target.");
    }
}
