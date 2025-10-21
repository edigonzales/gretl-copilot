package ch.so.agi.gretl.copilot.retrieval;

public record RetrievedDocument(String category, String title, String snippet, String url, double score) {
}
