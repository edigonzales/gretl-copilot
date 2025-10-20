package ch.so.agi.gretl.copilot.session;

import java.time.Instant;
import java.util.UUID;

public class ChatMessage {
    private final UUID id;
    private final ChatRole role;
    private final String content;
    private final Instant timestamp;

    public ChatMessage(ChatRole role, String content) {
        this(UUID.randomUUID(), role, content, Instant.now());
    }

    public ChatMessage(UUID id, ChatRole role, String content, Instant timestamp) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
