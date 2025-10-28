@param String userMessage
@param String sessionId
@param String messageId
@param ch.so.agi.gretl.copilot.chat.AssistantResponse assistantResponse
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
<article class="message message--assistant">
    <div class="message__content">
        <header class="message__header message__header--assistant">
            <span class="message__title">
                <img src="/gretl_logo_hexagon_small.png" width="25" alt="GRETL Copilot logo" />
                <span class="message__author">GRETL Copilot</span>
            </span>
            <span class="message__meta">Guided Task Assistant</span>
        </header>
        <div class="message__body">
            <div class="assistant-body" id="assistant-body-${messageId}">
                @if(assistantResponse != null && assistantResponse.hasMarkdown())
                    @assistantResponse.markdownHtml()
                @endif
                @if(assistantResponse != null && assistantResponse.hasCodeBlock())
                    @assistantResponse.codeBlockHtml()
                @endif
                @if(assistantResponse != null && assistantResponse.hasLinks())
                    @assistantResponse.linksHtml()
                @endif
            </div>
        </div>
    </div>
</article>
