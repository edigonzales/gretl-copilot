package ch.so.agi.gretl.chat.service;

import ch.so.agi.gretl.chat.model.ChatMessage;
import ch.so.agi.gretl.chat.model.RagContext;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private final IntentClassifier intentClassifier;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;

    public ChatService(IntentClassifier intentClassifier, RetrievalService retrievalService, LlmClient llmClient) {
        this.intentClassifier = intentClassifier;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
    }

    public Flux<String> streamResponse(List<ChatMessage> history, String prompt) {
        String intent = intentClassifier.classify(prompt);
        RagContext context = retrievalService.retrieve(intent, prompt);
        return llmClient.streamCompletion(context, history, prompt);
    }
}
