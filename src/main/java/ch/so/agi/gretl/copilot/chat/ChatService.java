package ch.so.agi.gretl.copilot.chat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public AssistantReply handleUserMessage(String sessionId, String userMessage) {
        ChatSession session = sessionRegistry.getOrCreate(sessionId);
        ChatMessage message = new ChatMessage(ChatRole.USER, userMessage);
        session.addMessage(message);

        ChatMessage assistantPlaceholder = new ChatMessage(ChatRole.ASSISTANT, "");
        session.addMessage(assistantPlaceholder);
        UUID messageId = assistantPlaceholder.getId();

        IntentClassification classification = intentClassifier.classify(userMessage);
        RetrievalResult retrievalResult = retrievalService.retrieve(userMessage, classification);
        List<RetrievedDocument> documents = (retrievalResult != null && retrievalResult.documents() != null)
                ? retrievalResult.documents()
                : List.of();
        CopilotPrompt prompt = new CopilotPrompt(userMessage, classification, documents);

        List<CopilotStreamSegment> segments = modelClient.generateResponse(prompt);
        return buildAssistantReply(session, sessionId, assistantPlaceholder, retrievalResult, segments);
    }

    private AssistantReply buildAssistantReply(ChatSession session, String sessionId, ChatMessage assistantMessage,
            RetrievalResult retrievalResult, List<CopilotStreamSegment> segments) {
        UUID messageId = assistantMessage.getId();
        List<String> htmlFragments = new ArrayList<>();
        StringBuilder textAccumulator = new StringBuilder();
        StringBuilder sessionContent = new StringBuilder();

        for (CopilotStreamSegment segment : segments) {
            if (segment == null || segment.type() == null) {
                continue;
            }

            String rawContent = Optional.ofNullable(segment.content()).map(String::strip).orElse("");

            switch (segment.type()) {
            case TEXT -> {
                if (rawContent.isEmpty()) {
                    break;
                }
                if (sessionContent.length() > 0) {
                    sessionContent.append("\n\n");
                }
                sessionContent.append(rawContent);
                if (textAccumulator.length() > 0) {
                    textAccumulator.append("\n\n");
                }
                textAccumulator.append(rawContent);
            }
            case CODE_BLOCK -> {
                flushText(messageId, textAccumulator, htmlFragments);
                if (rawContent.isEmpty()) {
                    break;
                }
                session.registerBuildGradle(messageId, rawContent);
                if (sessionContent.length() > 0) {
                    sessionContent.append("\n\n");
                }
                sessionContent.append("```gradle\n").append(rawContent).append("\n```");
                String codeId = "code-" + messageId;
                htmlFragments.add(buildCodeBlockHtml(sessionId, messageId, codeId, rawContent));
            }
            case LINKS -> {
                flushText(messageId, textAccumulator, htmlFragments);
                List<RetrievedDocument> documents = (retrievalResult == null || retrievalResult.documents() == null)
                        ? List.of()
                        : retrievalResult.documents();
                if (documents.isEmpty()) {
                    break;
                }
                String linksHtml = buildLinksHtml(documents);
                htmlFragments.add(linksHtml);
                if (sessionContent.length() > 0) {
                    sessionContent.append("\n\n");
                }
                sessionContent.append(stripHtmlTags(linksHtml));
            }
            }
        }

        flushText(messageId, textAccumulator, htmlFragments);

        assistantMessage.replaceContent(sessionContent.toString().trim());

        String combinedHtml = String.join("", htmlFragments);
        return new AssistantReply(messageId, new HtmlFragment(combinedHtml));
    }

    private void flushText(UUID messageId, StringBuilder textAccumulator, List<String> htmlFragments) {
        if (textAccumulator.isEmpty()) {
            return;
        }
        String markdownHtml = renderMarkdownHtml(messageId, textAccumulator.toString());
        if (!markdownHtml.isEmpty()) {
            htmlFragments.add(markdownHtml);
        }
        textAccumulator.setLength(0);
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
        builder.append("<div class=\"assistant-body__markdown\" data-message-id=\"").append(messageId)
                .append("\">");
        builder.append("<div class=\"markdown-body\">");
        builder.append(renderedMarkdown);
        builder.append("</div>");
        builder.append("</div>");
        return builder.toString();
    }

    private String renderMarkdownHtml(UUID messageId, String markdownSource) {
        if (markdownSource == null) {
            return "";
        }
        String source = markdownSource.trim();
        if (source.isEmpty()) {
            return "";
        }
        String rendered = markdownRenderer.render(source);
        if (rendered.isBlank()) {
            return "";
        }
        return buildMarkdownHtml(messageId, rendered);
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
        return session.findBuildGradle(messageId).map(content -> content.getBytes(StandardCharsets.UTF_8));
    }
}
