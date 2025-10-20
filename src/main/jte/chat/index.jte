@import java.util.List
@import ch.so.agi.gretl.chat.model.ChatMessage
@param String buildGradleSnippet
@param List conversation
@param String theme
@param boolean lightTheme
<!DOCTYPE html>
<html lang="en" data-theme="${lightTheme ? "light" : "dark"}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Chat with a GRETL-focused AI copilot">
    <title>GRETL Copilot</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://unpkg.com/htmx.org@1.9.12"></script>
    <script src="https://unpkg.com/htmx.org@1.9.12/dist/ext/sse.js"></script>
    <link rel="stylesheet" href="/css/chat.css">
</head>
<body>
<div class="app-shell">
    <header class="app-header">
        <div class="header-inner">
            <div class="brand">
                <span class="brand-subtitle">GRETL Copilot</span>
                <h1 class="brand-title">Guided Task Assistant</h1>
            </div>
            <nav class="header-links">
                <a href="https://gretl.app" target="_blank" rel="noreferrer">Docs</a>
                <a href="https://github.com/sogis/gretl" target="_blank" rel="noreferrer">GitHub</a>
                <button type="button"
                        class="theme-toggle"
                        hx-post="/theme"
                        hx-vals='{"theme":"${lightTheme ? "dark" : "light"}"}'
                        hx-target="body"
                        hx-swap="none"
                        aria-label="Switch to ${lightTheme ? "dark" : "light"} mode"
                        aria-pressed="${lightTheme ? "false" : "true"}">
                    @if(lightTheme)
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364-1.414 1.414M8.05 17.95l-1.414 1.414m12.728 0-1.414-1.414M8.05 6.05 6.636 4.636" />
                            <circle cx="12" cy="12" r="3" />
                        </svg>
                    @else
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
                        </svg>
                    @endif
                </button>
            </nav>
        </div>
    </header>
    <main>
        <section class="chat-area">
            <div class="chat-container">
                <div id="chat-alert" class="chat-alert" aria-live="polite"></div>
                <div id="chat-messages" class="chat-feed" aria-live="polite" hx-on::afterSwap="this.scrollTop = this.scrollHeight;">
                    @if(conversation.isEmpty())
                        <div class="empty-state">
                            <p>Ask for GRETL tasks, workflows, or best practices to get started.</p>
                        </div>
                    @else
                        @for(ChatMessage chatMessage : (List<ChatMessage>) conversation)
                            <div class="message-row" data-role="${chatMessage.role()}">
                                <div class="message ${"user".equals(chatMessage.role()) ? "message-user" : "message-assistant"}">
                                    <p>${chatMessage.content()}</p>
                                </div>
                            </div>
                        @endfor
                    @endif
                </div>
                <form id="chat-form"
                      class="chat-form"
                      hx-post="/chat/send"
                      hx-target="#chat-messages"
                      hx-swap="beforeend"
                      hx-indicator="#chat-indicator"
                      hx-on::afterRequest="this.reset(); htmx.find('#prompt').focus();">
                    <label for="prompt" class="sr-only">Ask the assistant</label>
                    <div class="composer-input">
                        <textarea id="prompt" name="prompt" rows="3" placeholder="Ask for a GRETL task…"></textarea>
                        <button type="submit" class="submit-button" aria-label="Send message">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M3 4.5 21 12 3 19.5 6.75 12 3 4.5z" />
                            </svg>
                        </button>
                    </div>
                    <div class="chat-form-meta">
                        <p>Prototype: responses are mocked until RAG pipeline is connected.</p>
                        <div id="chat-indicator" class="htmx-indicator">
                            <span class="indicator-dot"></span>
                            Sending prompt…
                        </div>
                    </div>
                </form>
            </div>
        </section>
    </main>
    <footer>
        <div class="footer-inner">
            <p>Prototype RAG assistant for GRETL documentation.</p>
            <p>Streaming powered by Spring AI (mocked).</p>
        </div>
    </footer>
</div>
</body>
</html>
