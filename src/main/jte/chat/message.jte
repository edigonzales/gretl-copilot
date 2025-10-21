@param String userMessage
@param String sessionId
@param String messageId
<article class="message message--user">
    <div class="message__content">
        <header class="message__header">
            <span class="message__author">You</span>
        </header>
        <div class="message__body">
            <p>${userMessage}</p>
        </div>
    </div>
</article>
<article class="message message--assistant" hx-ext="sse" sse-connect="/chat/stream/${sessionId}/${messageId}"
    sse-close="complete">
    <div class="message__content">
        <header class="message__header message__header--assistant">
            <span class="message__title">
                <img src="/gretl_logo_hexagon_small.png" width="25" alt="GRETL Copilot logo" />
                <span class="message__author">GRETL Copilot</span>
            </span>
            <span class="message__meta">Guided Task Assistant</span>
        </header>
        <div class="message__body">
            <div class="message__stream">
                <div class="typing-indicator typing-indicator--inline">
                    <span></span><span></span><span></span>
                </div>
                <div class="assistant-body" id="assistant-body-${messageId}" sse-swap="message" hx-swap="beforeend"></div>
            </div>
        </div>
    </div>
</article>
