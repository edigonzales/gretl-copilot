package ch.so.agi.gretl.chat.model;

public record ChatMessage(String role, String content) {
    public static ChatMessage ofUser(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage ofAssistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
