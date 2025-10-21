@param String sessionId
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GRETL Copilot</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/css/chat.css">
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.7/dist/htmx.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/htmx-ext-sse@2.2.4/sse.min.js"></script>
</head>
<body>
    <main class="app-shell">
        <header class="top-bar">
            <div class="brand">
                <div class="brand__badge">
                    <img src="/gretl_logo_hexagon_small.png" alt="GRETL Copilot logo" />
                </div>
                <div class="brand__text">
                    <p class="brand__name">GRETL COPILOT</p>
                    <p class="brand__tagline">Guided Task Assistant</p>
                </div>
            </div>
            <nav class="top-bar__links">
                <a href="https://gretl.app" target="_blank" rel="noopener" class="top-bar__link"><strong>gretl.app</strong></a>
                <a href="https://github.com/sogis/gretl" target="_blank" rel="noopener" class="top-bar__link"><strong>github.com/sogis/gretl</strong></a>
            </nav>
        </header>
        <section class="chat-wrapper">
            <div class="conversation" id="conversation">
                <article class="message message--assistant message--intro">
                    <div class="message__content">
                        <header class="message__header message__header--assistant">
                            <span class="message__title">
                                <img src="/gretl_logo_hexagon_small.png" alt="GRETL Copilot logo" />
                                <span class="message__author">GRETL Copilot</span>
                            </span>
                            <span class="message__meta">Guided Task Assistant</span>
                        </header>
                        <div class="message__body">
                            <p>Need to orchestrate INTERLIS imports, publish layers or automate QA? Describe your goal and GRETL Copilot will assemble the task, parameters and code to run it.</p>
                            <p class="message__hint">Tip: Paste URLs, dataset names or target schemas &mdash; Copilot will classify, retrieve and draft the runnable Gradle task for you.</p>
                        </div>
                    </div>
                </article>
            </div>
        </section>
        <section class="composer">
            <form class="composer__form" hx-post="/chat/message" hx-target="#conversation" hx-swap="beforeend"
                hx-on::after-request="this.reset();this.querySelector('textarea').focus();" hx-indicator="#typing-indicator">
                <input type="hidden" name="sessionId" value="${sessionId}">
                <div class="composer__field">
                    <label class="sr-only" for="message">Message</label>
                    <textarea id="message" name="message" rows="4" required placeholder="Ask GRETL Copilot how to automate your next workflow..."
                        autocomplete="off"></textarea>
                    <button type="submit" class="btn btn--primary" aria-label="Send message">
                        <span aria-hidden="true">&#8593;</span>
                    </button>
                </div>
            </form>
            <div id="typing-indicator" class="typing-indicator htmx-indicator">
                <span></span><span></span><span></span>
            </div>
        </section>
    </main>
    <script>
        document.addEventListener('htmx:afterSwap', function (event) {
            if (event.detail.target && event.detail.target.id === 'conversation') {
                event.detail.target.scrollTop = event.detail.target.scrollHeight;
            }
        });

        document.body.addEventListener('htmx:sseMessage', function (event) {
            if (!event.detail) {
                return;
            }

            var messageType = event.detail.type;
            var container = event.target;

            if (messageType === 'message' && container) {
                var assistantBody = container.querySelector('.assistant-body');
                if (assistantBody) {
                    var indicator = assistantBody.previousElementSibling;
                    if (indicator && indicator.classList.contains('typing-indicator')) {
                        indicator.remove();
                    }
                }
            }

            if (messageType === 'complete' && container) {
                container.removeAttribute('sse-connect');
                container.removeAttribute('data-sse-connect');
                container.removeAttribute('sse-close');
                container.removeAttribute('data-sse-close');
            }

            var conversation = document.getElementById('conversation');
            if (conversation) {
                conversation.scrollTop = conversation.scrollHeight;
            }
        });

        document.body.addEventListener('htmx:sseClose', function (event) {
            var container = event.target;
            if (!container) {
                return;
            }
            container.removeAttribute('sse-connect');
            container.removeAttribute('data-sse-connect');
            container.removeAttribute('sse-close');
            container.removeAttribute('data-sse-close');
        });

        document.addEventListener('click', function (event) {
            var button = event.target.closest('[data-copy-source]');
            if (!button) {
                return;
            }
            var sourceId = button.getAttribute('data-copy-source');
            var source = document.getElementById(sourceId);
            if (!source) {
                return;
            }
            var text = source.innerText;
            navigator.clipboard.writeText(text).then(function () {
                button.classList.add('btn--copied');
                button.textContent = 'Copied';
                setTimeout(function () {
                    button.classList.remove('btn--copied');
                    button.textContent = 'Copy';
                }, 1800);
            });
        });

        (function () {
            var textarea = document.getElementById('message');
            if (!textarea) {
                return;
            }

            var baseHeight = textarea.scrollHeight;

            function autoResize() {
                textarea.style.height = 'auto';
                textarea.style.height = Math.max(baseHeight, textarea.scrollHeight) + 'px';
            }

            textarea.addEventListener('input', autoResize);
            autoResize();

            document.body.addEventListener('htmx:afterRequest', function (event) {
                if (event.target && event.target.matches('.composer__form')) {
                    textarea.style.height = baseHeight + 'px';
                }
            });
        })();
    </script>
</body>
</html>
