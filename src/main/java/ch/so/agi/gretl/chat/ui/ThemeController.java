package ch.so.agi.gretl.chat.ui;

import ch.so.agi.gretl.chat.session.ChatSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ThemeController {

    private final ChatSessionService chatSessionService;

    public ThemeController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @PostMapping("/theme")
    public ResponseEntity<Void> updateTheme(@RequestParam("theme") String theme, HttpSession session) {
        chatSessionService.updateTheme(session, theme);
        return ResponseEntity.noContent()
                .header("HX-Refresh", "true")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
