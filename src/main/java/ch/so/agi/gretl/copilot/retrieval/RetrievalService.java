package ch.so.agi.gretl.copilot.retrieval;

import ch.so.agi.gretl.copilot.intent.IntentClassification;

public interface RetrievalService {
    RetrievalResult retrieve(String userMessage, IntentClassification classification);
}
