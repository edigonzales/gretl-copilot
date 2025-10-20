package ch.so.agi.gretl.chat.service;

import ch.so.agi.gretl.chat.model.RagContext;

public interface RetrievalService {
    RagContext retrieve(String intentLabel, String prompt);
}
