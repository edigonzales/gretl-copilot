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
    <script src="/js/clipboard.js" defer></script>
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
        <section class="chat-section">
            <div class="chat-section-inner">
                <div class="chat-layout">
                    <div class="chat-panel">
                        <div class="chat-panel-header">
                            <div>
                                <h2>Chat workspace</h2>
                                <p>Ask for GRETL tasks, workflows, and best practices.</p>
                            </div>
                            <div class="stream-status">
                                <span class="stream-status-dot" aria-hidden="true"></span>
                                Streaming
                            </div>
                        </div>
                        <div id="chat-alert" aria-live="polite"></div>
                        <div id="chat-messages" class="chat-messages" aria-live="polite" hx-on::afterSwap="this.scrollTop = this.scrollHeight;">
                            @if(conversation.isEmpty())
                                <div class="empty-state">
                                    <p>Start the conversation with a concrete task, e.g.</p>
                                    <strong>“Import INTERLIS data into PostGIS and prepare a sample job.”</strong>
                                </div>
                            @else
                                @for(ChatMessage chatMessage : (List<ChatMessage>) conversation)
                                    <div class="message-row" data-role="${chatMessage.role()}">
                                        <div class="message-bubble">
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
                            <textarea id="prompt" name="prompt" rows="3" placeholder="Ask for a GRETL task…"></textarea>
                            <div class="chat-form-controls">
                                <div class="chat-form-footer">
                                    <button type="submit" class="submit-button">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M3 4.5 21 12 3 19.5 6.75 12 3 4.5z" />
                                        </svg>
                                    </button>
                                    <p>Prototype: responses are mocked until RAG pipeline is connected.</p>
                                </div>
                                <div id="chat-indicator" class="htmx-indicator">
                                    <span class="stream-status-dot"></span>
                                    Sending prompt…
                                </div>
                            </div>
                        </form>
                    </div>
                    <aside class="aside">
                        <section class="aside-card">
                            <div class="snippet-header">
                                <h2>build.gradle snippet</h2>
                                <p>Generated by the assistant. Copy or download for reuse.</p>
                            </div>
                            <div class="snippet-actions">
                                <button type="button"
                                        class="copy-button"
                                        data-clipboard-target="#build-gradle-snippet"
                                        data-clipboard-message-target="#copy-feedback"
                                        data-clipboard-success="build.gradle snippet copied"
                                        aria-label="Copy build.gradle">
                                    <span class="sr-only">Copy build.gradle</span>
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                        <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                                        <path d="M5 15H4a2 2 0 0 1-2-2V4c0-1.1.9-2 2-2h9c1.1 0 2 .9 2 2v1" />
                                    </svg>
                                </button>
                                <a class="download-button" href="/snippets/build.gradle" download>
                                    <span class="sr-only">Download build.gradle</span>
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
                                        <path d="M7 10l5 5 5-5" />
                                        <path d="M12 15V3" />
                                    </svg>
                                </a>
                            </div>
                            <pre id="build-gradle-snippet" class="snippet"><code>${buildGradleSnippet}</code></pre>
                            <p id="copy-feedback" class="sr-only" aria-live="polite"></p>
                            <p class="snippet-note">Copy via the button or download the file to adapt it locally.</p>
                        </section>
                        <section class="aside-card">
                            <h2>How responses will be generated</h2>
                            <ol>
                                <li>Classify the intent (e.g. <span class="intent-example">import INTERLIS → PostGIS</span>).</li>
                                <li>Retrieve relevant tasks, properties, and examples with BM25 + re-ranking.</li>
                                <li>Compose the answer and code with Spring AI guardrails.</li>
                            </ol>
                            <p>The current build simulates the pipeline so that the UI and streaming experience can be validated before wiring the live RAG components.</p>
                        </section>
                    </aside>
                </div>
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
