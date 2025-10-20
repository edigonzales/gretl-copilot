package ch.so.agi.gretl.copilot.model;

import java.util.List;

import ch.so.agi.gretl.copilot.intent.IntentClassification;
import ch.so.agi.gretl.copilot.retrieval.RetrievedDocument;

public record CopilotPrompt(String userMessage, IntentClassification classification,
        List<RetrievedDocument> documents) {
}
