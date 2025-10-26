package ch.so.agi.gretl.copilot.intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record IntentClassification(String label, double confidence, String rationale,
        List<IntentLabel> secondaryLabels) {

    public IntentClassification {
        secondaryLabels = secondaryLabels == null ? List.of() : List.copyOf(secondaryLabels);
    }

    public List<IntentLabel> allLabels() {
        List<IntentLabel> labels = new ArrayList<>();
        if (label != null) {
            labels.add(new IntentLabel(label, confidence));
        }
        labels.addAll(secondaryLabels);
        return Collections.unmodifiableList(labels);
    }
}