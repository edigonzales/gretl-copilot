package ch.so.agi.gretl.copilot.model;

import reactor.core.publisher.Flux;

public interface CopilotModelClient {
    Flux<CopilotStreamSegment> streamResponse(CopilotPrompt prompt);
}
