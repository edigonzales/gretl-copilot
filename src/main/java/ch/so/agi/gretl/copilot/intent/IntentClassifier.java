package ch.so.agi.gretl.copilot.intent;

public interface IntentClassifier {
    IntentClassification classify(String userMessage);
}
