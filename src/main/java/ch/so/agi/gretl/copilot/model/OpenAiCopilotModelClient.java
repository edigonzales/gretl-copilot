package ch.so.agi.gretl.copilot.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.so.agi.gretl.copilot.prompt.CopilotPromptBuilder;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(name = "gretl.copilot.model.provider", havingValue = "openai", matchIfMissing = false)
public class OpenAiCopilotModelClient implements CopilotModelClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCopilotModelClient.class);

    private static final Pattern GRADLE_CODE_BLOCK = Pattern.compile("```gradle\\s*(.*?)```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final ChatModel chatModel;
    private final CopilotPromptBuilder promptBuilder;

    public OpenAiCopilotModelClient(ChatModel chatModel, CopilotPromptBuilder promptBuilder) {
        this.chatModel = chatModel;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public Flux<CopilotStreamSegment> streamResponse(CopilotPrompt prompt) {
        if (log.isDebugEnabled()) {
            log.debug("streamResponse() building prompt for intent {} with {} documents",
                    prompt.classification() != null ? prompt.classification().label() : "<none>",
                    prompt.documents() == null ? 0 : prompt.documents().size());
        }
        Prompt llmPrompt = promptBuilder.build(prompt);

        String content;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Invoking ChatModel {} via call()", chatModel.getClass().getSimpleName());
            }
            content = chatModel.call(llmPrompt).getResult().getOutput().getText();
            if (log.isDebugEnabled()) {
                String preview = content == null ? "" : content.substring(0, Math.min(content.length(), 200));
                log.debug("ChatModel call completed; received {} characters. Preview: {}", content == null ? 0 : content.length(),
                        preview);
            }
        } catch (Exception ex) {
            log.error("Chat model invocation failed", ex);
            return Flux.error(ex);
        }

        List<CopilotStreamSegment> segments = toSegments(content, prompt);
        if (log.isDebugEnabled()) {
            log.debug("streamResponse() produced {} segments", segments.size());
        }
        return Flux.fromIterable(segments);
    }

    private List<CopilotStreamSegment> toSegments(String rawContent, CopilotPrompt prompt) {
        List<CopilotStreamSegment> segments = new ArrayList<>();

        if (rawContent == null) {
            return segments;
        }

        String textContent = rawContent.trim();
        String codeBlock = null;

        Matcher matcher = GRADLE_CODE_BLOCK.matcher(textContent);
        if (matcher.find()) {
            codeBlock = matcher.group(1).strip();
            textContent = matcher.replaceFirst("").trim();
        }

        if (!textContent.isEmpty()) {
            segments.add(new CopilotStreamSegment(CopilotStreamSegment.SegmentType.TEXT, textContent));
        }

        if (codeBlock != null && !codeBlock.isEmpty()) {
            segments.add(new CopilotStreamSegment(CopilotStreamSegment.SegmentType.CODE_BLOCK, codeBlock));
        }

        if (prompt.documents() != null && !prompt.documents().isEmpty()) {
            segments.add(new CopilotStreamSegment(CopilotStreamSegment.SegmentType.LINKS, ""));
        }

        return segments;
    }
}