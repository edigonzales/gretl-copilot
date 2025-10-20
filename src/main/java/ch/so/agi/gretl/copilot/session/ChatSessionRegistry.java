package ch.so.agi.gretl.copilot.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ChatSessionRegistry {
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatSession getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, ChatSession::new);
    }

    public ChatSession create() {
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(sessionId);
        sessions.put(sessionId, session);
        return session;
    }
}
