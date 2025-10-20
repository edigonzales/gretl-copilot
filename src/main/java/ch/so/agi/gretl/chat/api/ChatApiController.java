package ch.so.agi.gretl.chat.api;

import ch.so.agi.gretl.chat.service.ChatService;
import ch.so.agi.gretl.chat.session.ChatSessionService;
import ch.so.agi.gretl.chat.session.ChatSessionService.PendingResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ChatSessionService chatSessionService;

    public ChatApiController(ChatService chatService, ChatSessionService chatSessionService) {
        this.chatService = chatService;
        this.chatSessionService = chatSessionService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam(name = "token", required = false) String token,
            HttpSession session) {

        if (!StringUtils.hasText(token)) {
            return errorResponse("The streaming session expired. Please try again.");
        }

        PendingResponse pending = chatSessionService.pendingResponses(session).remove(token);
        if (pending == null) {
            return errorResponse("The response is no longer available. Send your prompt again.");
        }

        List<ch.so.agi.gretl.chat.model.ChatMessage> history = pending.history();
        String prompt = pending.prompt();
        StringBuilder assistantMessage = new StringBuilder();

        return chatService.streamResponse(history, prompt)
                .doOnNext(assistantMessage::append)
                .map(delta -> ServerSentEvent.<String>builder(HtmlUtils.htmlEscape(delta)).event("delta").build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder("complete").event("done").build()))
                .doOnComplete(() -> chatSessionService.conversation(session)
                        .add(ch.so.agi.gretl.chat.model.ChatMessage.ofAssistant(assistantMessage.toString())));
    }

    private Flux<ServerSentEvent<String>> errorResponse(String message) {
        return Flux.just(
                ServerSentEvent.<String>builder(HtmlUtils.htmlEscape(message)).event("delta").build(),
                ServerSentEvent.<String>builder("complete").event("done").build());
    }
}
