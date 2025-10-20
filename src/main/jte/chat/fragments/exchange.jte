@import ch.so.agi.gretl.chat.model.ChatMessage
@param ChatMessage userMessage
@param String streamId
@param boolean lightTheme
<div hx-swap-oob="true" id="chat-alert"></div>
<div class="flex justify-end" data-role="user">
    <div class="max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${lightTheme ? "bg-emerald-500/15 text-emerald-900 border border-emerald-500/40" : "bg-emerald-500/20 text-emerald-100"}">
        <p class="whitespace-pre-wrap">${userMessage.content()}</p>
    </div>
</div>
<div class="flex justify-start" data-role="assistant"
     hx-ext="sse"
     sse-connect="/api/chat/stream?token=${streamId}">
    <div class="assistant-stream max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${lightTheme ? "bg-white text-slate-900 border border-slate-200 shadow-sm" : "bg-slate-800/80 text-slate-100"}">
        <p class="whitespace-pre-wrap" sse-swap="delta" hx-swap="beforeend" aria-live="polite"></p>
    </div>
</div>
