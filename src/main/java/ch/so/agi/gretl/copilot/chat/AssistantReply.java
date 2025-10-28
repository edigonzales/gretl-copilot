package ch.so.agi.gretl.copilot.chat;

import java.util.UUID;

public record AssistantReply(UUID messageId, HtmlFragment content) {
}
