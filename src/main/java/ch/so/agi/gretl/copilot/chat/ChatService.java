package ch.so.agi.gretl.copilot.chat;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import ch.so.agi.gretl.copilot.intent.IntentClassification;
import ch.so.agi.gretl.copilot.intent.IntentClassifier;
import ch.so.agi.gretl.copilot.model.CopilotModelClient;
import ch.so.agi.gretl.copilot.model.CopilotPrompt;
import ch.so.agi.gretl.copilot.model.CopilotStreamSegment;
import ch.so.agi.gretl.copilot.retrieval.RetrievalResult;
import ch.so.agi.gretl.copilot.retrieval.RetrievedDocument;
import ch.so.agi.gretl.copilot.retrieval.RetrievalService;
import ch.so.agi.gretl.copilot.session.ChatMessage;
import ch.so.agi.gretl.copilot.session.ChatRole;
import ch.so.agi.gretl.copilot.session.ChatSession;
import ch.so.agi.gretl.copilot.session.ChatSessionRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ChatService {
    private final ChatSessionRegistry sessionRegistry;
    private final IntentClassifier intentClassifier;
    private final RetrievalService retrievalService;
    private final CopilotModelClient modelClient;

    public ChatService(ChatSessionRegistry sessionRegistry, IntentClassifier intentClassifier,
            RetrievalService retrievalService, CopilotModelClient modelClient) {
        this.sessionRegistry = sessionRegistry;
        this.intentClassifier = intentClassifier;
        this.retrievalService = retrievalService;
        this.modelClient = modelClient;
    }

    public UUID handleUserMessage(String sessionId, String userMessage) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        ChatMessage message = new ChatMessage(ChatRole.USER, userMessage);
        session.addMessage(message);

        ChatMessage assistantPlaceholder = new ChatMessage(ChatRole.ASSISTANT, "");
        session.addMessage(assistantPlaceholder);
        return assistantPlaceholder.getId();
    }

    public Flux<ServerSentEvent<String>> streamAssistantResponse(String sessionId, UUID messageId) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        ChatMessage assistantMessage = session.findMessage(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant message"));

        List<ChatMessage> messages = session.getMessages();
        int messageIndex = messages.indexOf(assistantMessage);
        String userMessage = messageIndex > 0 ? messages.get(messageIndex - 1).getContent() : "";

        IntentClassification classification = intentClassifier.classify(userMessage);
        RetrievalResult retrievalResult = retrievalService.retrieve(userMessage, classification);
        CopilotPrompt prompt = new CopilotPrompt(userMessage, classification, retrievalResult.documents());

        return modelClient.streamResponse(prompt)
                .concatMap(segment -> mapSegment(sessionId, messageId, session, retrievalResult, segment));
    }

    private Flux<ServerSentEvent<String>> mapSegment(String sessionId, UUID messageId, ChatSession session,
            RetrievalResult retrievalResult, CopilotStreamSegment segment) {
        return switch (segment.type()) {
        case TEXT -> Flux.just(toMessageEvent("<span class=\"assistant-token\">" + escapeHtml(segment.content())
                + " </span>"));
        case CODE_BLOCK -> {
            session.registerBuildGradle(messageId, segment.content());
            String codeId = "code-" + messageId;
            String codeHtml = buildCodeBlockHtml(sessionId, messageId, codeId, segment.content());
            yield Flux.just(toMessageEvent(codeHtml));
        }
        case LINKS -> Flux.just(toMessageEvent(buildLinksHtml(retrievalResult.documents())));
        };
    }

    private ServerSentEvent<String> toMessageEvent(String html) {
        return ServerSentEvent.<String>builder().event("message").data(html).build();
    }

    private String buildCodeBlockHtml(String sessionId, UUID messageId, String codeId, String content) {
        return "<div class=\"code-card\">" + "<div class=\"code-card__header\">"
                + "<div class=\"code-card__title\">build.gradle</div>"
                + "<div class=\"code-card__actions\">"
                + "<button type=\"button\" class=\"btn btn--ghost\" data-copy-source=\"" + codeId
                + "\">Copy</button>" + "<a class=\"btn btn--primary\" href=\"/chat/download/" + sessionId + "/"
                + messageId + "\" download=\"build.gradle\">Download</a>" + "</div>" + "</div>"
                + "<pre id=\"" + codeId + "\"><code>" + escapeHtml(content) + "</code></pre>" + "</div>";
    }

    private String buildLinksHtml(List<RetrievedDocument> documents) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"retrieval-evidence\">");
        builder.append("<span class=\"retrieval-label\">Context sources</span>");
        builder.append("<ul class=\"retrieval-list\">");
        documents.stream().sorted(Comparator.comparingDouble(RetrievedDocument::score).reversed()).forEach(doc -> {
            builder.append("<li class=\"retrieval-item\">");
            builder.append("<span class=\"retrieval-pill\">" + escapeHtml(doc.category()) + "</span>");
            builder.append("<div class=\"retrieval-meta\">");
            builder.append("<a href=\"" + doc.url() + "\" target=\"_blank\">" + escapeHtml(doc.title())
                    + "</a>");
            builder.append("<p>" + escapeHtml(doc.snippet()) + "</p>");
            builder.append("</div>");
            builder.append("</li>");
        });
        builder.append("</ul>");
        builder.append("</div>");
        return builder.toString();
    }

    private String escapeHtml(String input) {
        StringBuilder sb = new StringBuilder();
        input.chars().forEach(ch -> {
            switch (ch) {
            case '<' -> sb.append("&lt;");
            case '>' -> sb.append("&gt;");
            case '"' -> sb.append("&quot;");
            case '\'' -> sb.append("&#39;");
            case '&' -> sb.append("&amp;");
            default -> sb.append((char) ch);
            }
        });
        return sb.toString();
    }

    public Mono<byte[]> loadBuildGradle(String sessionId, UUID messageId) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        return Mono.justOrEmpty(session.findBuildGradle(messageId))
                .map(content -> content.getBytes(StandardCharsets.UTF_8));
    }
}
