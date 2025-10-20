package ch.so.agi.gretl.chat.service.impl;

import ch.so.agi.gretl.chat.model.ChatMessage;
import ch.so.agi.gretl.chat.model.RagContext;
import ch.so.agi.gretl.chat.service.LlmClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class StubLlmClient implements LlmClient {

    @Override
    public Flux<String> streamCompletion(RagContext context, List<ChatMessage> history, String prompt) {
        List<String> chunks = new ArrayList<>();
        chunks.add("Here is a GRETL task that matches your intent: \n\n");
        for (String summary : context.retrievedTaskSummaries()) {
            chunks.add("• " + summary + "\n");
        }
        chunks.add("\nYou can embed the following task in your build.gradle:\n\n");
        chunks.add("task(\"importInterlis2Postgis\") {\n");
        chunks.add("    description = \"Imports INTERLIS data into PostGIS\"\n");
        chunks.add("    group = \"Database\"\n");
        chunks.add("    ili2pg {\n");
        chunks.add("        models = listOf(\"DM01AVCH24LV95D\")\n");
        chunks.add("        dataset = uri(\"https://example.com/fubar.xtf\")\n");
        chunks.add("        dbSchema = \"fubar\"\n");
        chunks.add("        dbUrl = env(\"POSTGIS_URL\")\n");
        chunks.add("    }\n");
        chunks.add("}\n\n");
        chunks.add("This stubbed response is streamed for UX testing. Configure the Spring AI OpenAI client and replace the stub to enable live completions.\n");
        return Flux.fromIterable(chunks).delayElements(Duration.ofMillis(180));
    }
}
