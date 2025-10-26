package ch.so.agi.gretl.copilot.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChatSession {
    private final String id;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<UUID, String> buildGradleArtifacts = new HashMap<>();
    private final Map<UUID, AssistantMessageView> assistantViews = new HashMap<>();

    public ChatSession(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public Optional<ChatMessage> findMessage(UUID messageId) {
        return messages.stream().filter(message -> message.getId().equals(messageId)).findFirst();
    }

    public void registerBuildGradle(UUID messageId, String content) {
        buildGradleArtifacts.put(messageId, content);
    }

    public Optional<String> findBuildGradle(UUID messageId) {
        return Optional.ofNullable(buildGradleArtifacts.get(messageId));
    }

    public synchronized void resetAssistantView(UUID messageId) {
        assistantViews.put(messageId, new AssistantMessageView());
    }

    public synchronized AssistantMessageView getAssistantView(UUID messageId) {
        return assistantViews.computeIfAbsent(messageId, id -> new AssistantMessageView());
    }
}
