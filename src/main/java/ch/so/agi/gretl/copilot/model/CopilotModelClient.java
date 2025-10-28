package ch.so.agi.gretl.copilot.model;

import java.util.List;

public interface CopilotModelClient {
    List<CopilotStreamSegment> generateResponse(CopilotPrompt prompt);
}
