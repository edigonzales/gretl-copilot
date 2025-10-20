package ch.so.agi.gretl.chat.service.impl;

import ch.so.agi.gretl.chat.service.IntentClassifier;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class StubIntentClassifier implements IntentClassifier {

    @Override
    public String classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "general.help";
        }
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("interlis") && lower.contains("postgis")) {
            return "import.interlis.postgis";
        }
        if (lower.contains("etl") || lower.contains("workflow")) {
            return "design.workflow";
        }
        return "general.help";
    }
}
