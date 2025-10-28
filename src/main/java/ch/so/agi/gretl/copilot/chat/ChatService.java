package ch.so.agi.gretl.copilot.chat;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import gg.jte.Content;

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
@Service
public class ChatService {
    private final ChatSessionRegistry sessionRegistry;
    private final IntentClassifier intentClassifier;
    private final RetrievalService retrievalService;
    private final CopilotModelClient modelClient;
    private final MarkdownRenderer markdownRenderer;

    public ChatService(ChatSessionRegistry sessionRegistry, IntentClassifier intentClassifier,
            RetrievalService retrievalService, CopilotModelClient modelClient, MarkdownRenderer markdownRenderer) {
        this.sessionRegistry = sessionRegistry;
        this.intentClassifier = intentClassifier;
        this.retrievalService = retrievalService;
        this.modelClient = modelClient;
        this.markdownRenderer = markdownRenderer;
    }

    public UUID handleUserMessage(String sessionId, String userMessage) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        ChatMessage message = new ChatMessage(ChatRole.USER, userMessage);
        session.addMessage(message);

        ChatMessage assistantPlaceholder = new ChatMessage(ChatRole.ASSISTANT, "");
        session.addMessage(assistantPlaceholder);
        return assistantPlaceholder.getId();
    }

    public AssistantResponse generateAssistantResponse(String sessionId, UUID messageId) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        ChatMessage assistantMessage = session.findMessage(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant message"));

        List<ChatMessage> messages = session.getMessages();
        int messageIndex = messages.indexOf(assistantMessage);
        String userMessage = messageIndex > 0 ? messages.get(messageIndex - 1).getContent() : "";

        IntentClassification classification = intentClassifier.classify(userMessage);
        RetrievalResult retrievalResult = retrievalService.retrieve(userMessage, classification);
        CopilotPrompt prompt = new CopilotPrompt(userMessage, classification, retrievalResult.documents());

        List<CopilotStreamSegment> segments = modelClient.generateResponse(prompt);

        StringBuilder markdownSource = new StringBuilder();
        String codeHtml = "";
        String linksHtml = "";

        for (CopilotStreamSegment segment : segments) {
            switch (segment.type()) {
            case TEXT -> {
                String text = segment.content().strip();
                if (!text.isEmpty()) {
                    if (!assistantMessage.getContent().isBlank()) {
                        assistantMessage.appendContent("\n\n");
                    }
                    assistantMessage.appendContent(text);
                    if (markdownSource.length() > 0) {
                        markdownSource.append("\n\n");
                    }
                    markdownSource.append(text);
                }
            }
            case CODE_BLOCK -> {
                String code = segment.content();
                session.registerBuildGradle(messageId, code);
                assistantMessage.appendContent("\n\n```gradle\n" + code + "\n```");
                String codeId = "code-" + messageId;
                codeHtml = buildCodeBlockHtml(sessionId, messageId, codeId, code);
            }
            case LINKS -> {
                linksHtml = buildLinksHtml(retrievalResult.documents());
                String plainLinks = stripHtmlTags(linksHtml);
                if (!plainLinks.isBlank()) {
                    if (!assistantMessage.getContent().isBlank()) {
                        assistantMessage.appendContent("\n\n");
                    }
                    assistantMessage.appendContent(plainLinks);
                    if (markdownSource.length() > 0) {
                        markdownSource.append("\n\n");
                    }
                    markdownSource.append(plainLinks);
                }
            }
            }
        }

        String markdownHtml = "";
        if (markdownSource.length() > 0) {
            markdownHtml = renderMarkdownHtml(messageId, markdownSource.toString());
        }

        Content markdownContent = toContent(markdownHtml);
        Content codeContent = toContent(codeHtml);
        Content linksContent = toContent(linksHtml);

        return new AssistantResponse(markdownContent, codeContent, linksContent);
    }

    private String renderMarkdownHtml(UUID messageId, String markdownSource) {
        String rendered = markdownRenderer.render(markdownSource);
        if (rendered.isBlank()) {
            return "";
        }

        return buildMarkdownHtml(messageId, rendered);
    }

    private String buildCodeBlockHtml(String sessionId, UUID messageId, String codeId, String content) {
        return "<div class=\"code-card\">" + "<div class=\"code-card__header\">"
                + "<div class=\"code-card__title\">build.gradle</div>"
                + "<div class=\"code-card__actions\">"
                + "<button type=\"button\" class=\"btn btn--ghost\" data-copy-source=\"" + codeId
                + "\">Copy</button>" + "<a class=\"btn btn--primary btn--icon\" href=\"/chat/download/" + sessionId + "/"
                + messageId
                + "\" download=\"build.gradle\" aria-label=\"Download build.gradle\">"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" viewBox=\"0 0 16 16\" aria-hidden=\"true\" focusable=\"false\">"
                + "<path d=\"M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5\"/>"
                + "<path d=\"M7.646 11.854a.5.5 0 0 0 .708 0l3-3a.5.5 0 0 0-.708-.708L8.5 10.293V1.5a.5.5 0 0 0-1 0v8.793L5.354 8.146a.5.5 0 1 0-.708.708l3 3z\"/>"
                + "</svg><span class=\"sr-only\">Download build.gradle</span></a>" + "</div>" + "</div>"
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

    private String buildMarkdownHtml(UUID messageId, String renderedMarkdown) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div id=\"assistant-markdown-").append(messageId)
                .append("\" class=\"assistant-body__markdown\"");
        builder.append(">");
        builder.append("<div class=\"markdown-body\">");
        builder.append(renderedMarkdown);
        builder.append("</div>");
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

    private String stripHtmlTags(String input) {
        return input.replaceAll("<[^>]+>", "");
    }

    public Optional<byte[]> loadBuildGradle(String sessionId, UUID messageId) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        return session.findBuildGradle(messageId)
                .map(content -> content.getBytes(StandardCharsets.UTF_8));
    }

    private Content toContent(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return output -> output.writeContent(html);
    }
}
