package ch.so.agi.gretl.chat.ui;

import ch.so.agi.gretl.chat.model.ChatMessage;
import ch.so.agi.gretl.chat.session.ChatSessionService;
import ch.so.agi.gretl.chat.session.ChatSessionService.PendingResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/chat")
public class ChatInteractionController {

    private final ChatSessionService chatSessionService;

    public ChatInteractionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @PostMapping(value = "/send", produces = MediaType.TEXT_HTML_VALUE)
    public String send(
            @RequestParam(name = "prompt", required = false) String prompt,
            HttpSession session,
            Model model) {

        String trimmedPrompt = prompt != null ? prompt.trim() : "";
        String theme = chatSessionService.theme(session);

        if (!StringUtils.hasText(trimmedPrompt)) {
            model.addAttribute("message", "Please describe your GRETL task so I can help.");
            model.addAttribute("lightTheme", "light".equals(theme));
            return "chat/fragments/validation";
        }

        List<ChatMessage> conversation = chatSessionService.conversation(session);
        ChatMessage userMessage = ChatMessage.ofUser(trimmedPrompt);
        conversation.add(userMessage);

        String streamId = UUID.randomUUID().toString();
        List<ChatMessage> historySnapshot = List.copyOf(conversation);
        chatSessionService.pendingResponses(session).put(streamId, new PendingResponse(trimmedPrompt, historySnapshot));

        model.addAttribute("userMessage", userMessage);
        model.addAttribute("streamId", streamId);
        model.addAttribute("lightTheme", "light".equals(theme));

        return "chat/fragments/exchange";
    }
}
