package ch.so.agi.gretl.chat.session;

import ch.so.agi.gretl.chat.model.ChatMessage;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatSessionService {

    private static final String CONVERSATION_SESSION_KEY = "chatConversation";
    private static final String PENDING_SESSION_KEY = "chatPendingResponses";
    private static final String THEME_SESSION_KEY = "chatTheme";

    private static final ChatMessage INTRO_MESSAGE = ChatMessage.ofAssistant(
            "👋 Hi! I'm your GRETL Copilot. Describe the task you want to automate and I'll craft the build.gradle snippet and explain the steps.");

    @SuppressWarnings("unchecked")
    public List<ChatMessage> conversation(HttpSession session) {
        List<ChatMessage> conversation = (List<ChatMessage>) session.getAttribute(CONVERSATION_SESSION_KEY);
        if (conversation == null) {
            conversation = Collections.synchronizedList(new ArrayList<>());
            conversation.add(INTRO_MESSAGE);
            session.setAttribute(CONVERSATION_SESSION_KEY, conversation);
        } else if (conversation.isEmpty()) {
            conversation.add(INTRO_MESSAGE);
        }
        return conversation;
    }

    @SuppressWarnings("unchecked")
    public Map<String, PendingResponse> pendingResponses(HttpSession session) {
        Map<String, PendingResponse> pending = (Map<String, PendingResponse>) session.getAttribute(PENDING_SESSION_KEY);
        if (pending == null) {
            pending = new ConcurrentHashMap<>();
            session.setAttribute(PENDING_SESSION_KEY, pending);
        }
        return pending;
    }

    public String theme(HttpSession session) {
        Object themeAttribute = session.getAttribute(THEME_SESSION_KEY);
        if (themeAttribute instanceof String themeValue && StringUtils.hasText(themeValue)) {
            return themeValue;
        }
        session.setAttribute(THEME_SESSION_KEY, "dark");
        return "dark";
    }

    public void updateTheme(HttpSession session, String theme) {
        if (StringUtils.hasText(theme) && ("light".equalsIgnoreCase(theme) || "dark".equalsIgnoreCase(theme))) {
            session.setAttribute(THEME_SESSION_KEY, theme.toLowerCase());
        }
    }

    public record PendingResponse(String prompt, List<ChatMessage> history) {}
}
