@import ch.so.agi.gretl.chat.model.ChatMessage
@param ChatMessage userMessage
@param String streamId
@param boolean lightTheme
<div hx-swap-oob="true" id="chat-alert"></div>
<div class="message-row" data-role="user">
    <div class="message-bubble">
        <p>${userMessage.content()}</p>
    </div>
</div>
<div class="message-row" data-role="assistant"
     hx-ext="sse"
     sse-connect="/api/chat/stream?token=${streamId}">
    <div class="assistant-stream message-bubble">
        <p sse-swap="delta" hx-swap="beforeend" aria-live="polite"></p>
    </div>
</div>
