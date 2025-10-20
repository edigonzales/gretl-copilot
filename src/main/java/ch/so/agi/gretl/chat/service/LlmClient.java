package ch.so.agi.gretl.chat.service;

import ch.so.agi.gretl.chat.model.ChatMessage;
import ch.so.agi.gretl.chat.model.RagContext;
import reactor.core.publisher.Flux;

public interface LlmClient {
    Flux<String> streamCompletion(RagContext context, java.util.List<ChatMessage> history, String prompt);
}
