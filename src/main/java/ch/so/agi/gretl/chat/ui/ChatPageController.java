package ch.so.agi.gretl.chat.ui;

import ch.so.agi.gretl.chat.session.ChatSessionService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class ChatPageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPageController.class);
    private final Resource buildGradleResource;
    private final ChatSessionService chatSessionService;

    public ChatPageController(
            @Value("classpath:static/snippets/build.gradle") Resource buildGradleResource,
            ChatSessionService chatSessionService) {
        this.buildGradleResource = buildGradleResource;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping("/")
    public String chat(Model model, HttpSession session) {
        String theme = chatSessionService.theme(session);
        model.addAttribute("buildGradleSnippet", loadSnippet());
        model.addAttribute("conversation", chatSessionService.conversation(session));
        model.addAttribute("theme", theme);
        model.addAttribute("lightTheme", "light".equals(theme));
        return "chat/index";
    }

    private String loadSnippet() {
        try {
            return StreamUtils.copyToString(buildGradleResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to load build.gradle snippet", e);
            return "// build.gradle snippet unavailable in prototype";
        }
    }
}
