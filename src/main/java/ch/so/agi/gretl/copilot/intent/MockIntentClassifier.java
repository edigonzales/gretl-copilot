package ch.so.agi.gretl.copilot.intent;

import org.springframework.stereotype.Component;

@Component
public class MockIntentClassifier implements IntentClassifier {
    @Override
    public IntentClassification classify(String userMessage) {
        return new IntentClassification("import-interlis-to-postgis", 0.87,
                "Matched keywords for INTERLIS import and PostGIS target.");
    }
}
