package ch.so.agi.gretl.copilot.session;

import java.time.Instant;
import java.util.UUID;

public class ChatMessage {
    private final UUID id;
    private final ChatRole role;
    private final Instant timestamp;
    private final StringBuilder content;

    public ChatMessage(ChatRole role, String content) {
        this(UUID.randomUUID(), role, content, Instant.now());
    }

    public ChatMessage(UUID id, ChatRole role, String content, Instant timestamp) {
        this.id = id;
        this.role = role;
        this.timestamp = timestamp;
        this.content = new StringBuilder(content);
    }

    public UUID getId() {
        return id;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getContent() {
        return content.toString();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public synchronized void appendContent(String addition) {
        this.content.append(addition);
    }

    public synchronized void replaceContent(String newContent) {
        this.content.setLength(0);
        this.content.append(newContent);
    }
}
