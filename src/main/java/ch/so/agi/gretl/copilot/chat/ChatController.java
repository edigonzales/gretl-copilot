package ch.so.agi.gretl.copilot.chat;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.so.agi.gretl.copilot.session.ChatSession;
import ch.so.agi.gretl.copilot.session.ChatSessionRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ChatController {
    private final ChatSessionRegistry sessionRegistry;
    private final ChatService chatService;

    public ChatController(ChatSessionRegistry sessionRegistry, ChatService chatService) {
        this.sessionRegistry = sessionRegistry;
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String home(Model model) {
        ChatSession session = sessionRegistry.create();
        model.addAttribute("sessionId", session.getId());
        return "chat/index";
    }

    @PostMapping(value = "/chat/message", produces = MediaType.TEXT_HTML_VALUE)
    public String postMessage(@RequestParam(name = "sessionId") String sessionId,
            @RequestParam(name = "message") String userMessage, Model model) {
        UUID messageId = chatService.handleUserMessage(sessionId, userMessage);
        model.addAttribute("userMessage", userMessage);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("messageId", messageId.toString());
        return "chat/message";
    }

    @GetMapping(value = "/chat/stream/{sessionId}/{messageId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public @ResponseBody Flux<ServerSentEvent<String>> streamAssistant(
            @PathVariable(name = "sessionId") String sessionId, @PathVariable(name = "messageId") UUID messageId) {
        return chatService.streamAssistantResponse(sessionId, messageId);
    }

    @GetMapping("/chat/download/{sessionId}/{messageId}")
    public Mono<ResponseEntity<byte[]>> downloadBuildGradle(@PathVariable(name = "sessionId") String sessionId,
            @PathVariable(name = "messageId") UUID messageId) {
        return chatService.loadBuildGradle(sessionId, messageId).map(content -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"build.gradle\"")
                .contentType(MediaType.TEXT_PLAIN).body(content)).defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
