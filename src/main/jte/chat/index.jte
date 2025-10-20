@import java.util.List
@import ch.so.agi.gretl.chat.model.ChatMessage
@param String buildGradleSnippet
@param List conversation
@param String theme
@param boolean lightTheme
<!DOCTYPE html>
<html lang="en" class="h-full ${lightTheme ? "bg-slate-100 text-slate-900" : "bg-slate-950 text-slate-100"}" data-theme="${lightTheme ? "light" : "dark"}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Chat with a GRETL-focused AI copilot">
    <title>GRETL Copilot</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com?plugins=forms,typography"></script>
    <script src="https://unpkg.com/htmx.org@1.9.12"></script>
    <script src="https://unpkg.com/htmx.org@1.9.12/dist/ext/sse.js"></script>
    <link rel="stylesheet" href="/css/chat.css">
    <script src="/js/clipboard.js" defer></script>
</head>
<body class="h-full antialiased ${lightTheme ? "bg-slate-100" : "bg-slate-950"}">
<div class="min-h-full flex flex-col">
    <header class="border-b ${lightTheme ? "border-slate-200 bg-white/75" : "border-slate-800 bg-slate-950/70"} backdrop-blur">
        <div class="mx-auto w-full max-w-6xl px-6 py-5 flex items-center justify-between">
            <div class="flex items-center gap-3">
                <span class="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-400">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m6-6H6" />
                    </svg>
                </span>
                <div>
                    <p class="text-sm font-semibold uppercase tracking-widest ${lightTheme ? "text-slate-500" : "text-slate-400"}">GRETL Copilot</p>
                    <h1 class="text-xl font-semibold">Guided Task Assistant</h1>
                </div>
            </div>
            <nav class="flex items-center gap-4 text-sm">
                <a class="${lightTheme ? "transition hover:text-emerald-600 text-slate-500" : "transition hover:text-emerald-400 text-slate-400"}" href="https://gretl.app" target="_blank" rel="noreferrer">Docs</a>
                <a class="${lightTheme ? "transition hover:text-emerald-600 text-slate-500" : "transition hover:text-emerald-400 text-slate-400"}" href="https://github.com/sogis/gretl" target="_blank" rel="noreferrer">GitHub</a>
                <button type="button"
                        class="flex h-10 w-10 items-center justify-center rounded-full border ${lightTheme ? "border-slate-200 bg-white/70 text-amber-500" : "border-slate-800 bg-slate-900/70 text-amber-300"} shadow-sm"
                        hx-post="/theme"
                        hx-vals='{"theme":"${lightTheme ? "dark" : "light"}"}'
                        hx-target="body"
                        hx-swap="none"
                        aria-label="Switch to ${lightTheme ? "dark" : "light"} mode"
                        aria-pressed="${lightTheme ? "false" : "true"}">
                    @if(lightTheme)
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.364-6.364-1.414 1.414M8.05 17.95l-1.414 1.414m12.728 0-1.414-1.414M8.05 6.05 6.636 4.636M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                        </svg>
                    @else
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z" />
                        </svg>
                    @endif
                </button>
            </nav>
        </div>
    </header>
    <main class="flex-1">
        <section class="relative">
            <div class="pointer-events-none absolute inset-x-0 top-0 mx-auto h-60 w-full max-w-5xl overflow-hidden">
                <div class="absolute inset-0 bg-gradient-to-br ${lightTheme ? "from-emerald-500/20 via-sky-500/20" : "from-emerald-500/20 via-sky-500/10"} to-transparent blur-3xl"></div>
            </div>
            <div class="relative mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-10 lg:py-16">
                <div class="grid gap-6 lg:grid-cols-[2fr,1fr] lg:gap-10">
                    <div class="flex flex-col rounded-3xl border ${lightTheme ? "border-slate-200 bg-white/90" : "border-slate-800/80 bg-slate-900/60"} shadow-2xl shadow-emerald-500/10 backdrop-blur">
                        <div class="flex items-center justify-between border-b ${lightTheme ? "border-slate-200" : "border-slate-800/60"} px-6 py-4">
                            <div>
                                <h2 class="text-lg font-semibold">Chat workspace</h2>
                                <p class="text-sm ${lightTheme ? "text-slate-500" : "text-slate-400"}">Ask for GRETL tasks, workflows, and best practices.</p>
                            </div>
                            <div class="flex items-center gap-2 text-xs ${lightTheme ? "text-slate-500" : "text-slate-400"}">
                                <span class="flex h-2 w-2 rounded-full ${lightTheme ? "bg-emerald-500" : "bg-emerald-400"}"></span>
                                Streaming
                            </div>
                        </div>
                        <div id="chat-alert" class="px-6 pt-4" aria-live="polite"></div>
                        <div id="chat-messages" class="flex-1 space-y-4 overflow-y-auto px-6 py-6" aria-live="polite" hx-on::afterSwap="this.scrollTop = this.scrollHeight;">
                            @if(conversation.isEmpty())
                                <div class="flex h-48 flex-col items-center justify-center rounded-2xl border border-dashed ${lightTheme ? "border-slate-200 bg-white/60" : "border-slate-700/80 bg-slate-900/30"} text-center">
                                    <div class="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-400">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m6-6H6" />
                                        </svg>
                                    </div>
                                    <div class="mt-4 max-w-sm space-y-2 text-sm ${lightTheme ? "text-slate-500" : "text-slate-400"}">
                                        <p>Start the conversation with a concrete task, e.g.</p>
                                        <p class="font-mono ${lightTheme ? "text-slate-600" : "text-slate-300"}">“Import INTERLIS data into PostGIS and prepare a sample job.”</p>
                                    </div>
                                </div>
                            @else
                                @for(ChatMessage chatMessage : (List<ChatMessage>) conversation)
                                    <div class="flex ${chatMessage.role().equals("user") ? "justify-end" : "justify-start"}" data-role="${chatMessage.role()}">
                                        <div class="max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${chatMessage.role().equals("user") ? (lightTheme ? "bg-emerald-500/15 text-emerald-900 border border-emerald-500/40" : "bg-emerald-500/20 text-emerald-100") : (lightTheme ? "bg-white text-slate-900 border border-slate-200 shadow-sm" : "bg-slate-800/80 text-slate-100")}">
                                            <p class="whitespace-pre-wrap">${chatMessage.content()}</p>
                                        </div>
                                    </div>
                                @endfor
                            @endif
                        </div>
                        <form id="chat-form" class="border-t ${lightTheme ? "border-slate-200" : "border-slate-800/60"} px-6 py-5"
                              hx-post="/chat/send"
                              hx-target="#chat-messages"
                              hx-swap="beforeend"
                              hx-indicator="#chat-indicator"
                              hx-on::afterRequest="this.reset(); htmx.find('#prompt').focus();"
                        >
                            <label for="prompt" class="sr-only">Ask the assistant</label>
                            <div class="flex items-end gap-3">
                                <div class="flex-1 rounded-2xl border ${lightTheme ? "border-slate-200 bg-white" : "border-slate-700/70 bg-slate-900/60"} px-4 py-3 text-sm shadow-inner ${lightTheme ? "shadow-slate-200/60" : "shadow-black/40"} focus-within:border-emerald-500 focus-within:ring-1 focus-within:ring-emerald-500/60">
                                    <textarea id="prompt" name="prompt" rows="2" placeholder="Ask for a GRETL task…" class="w-full resize-none border-none bg-transparent ${lightTheme ? "text-slate-900 placeholder:text-slate-400" : "text-slate-100 placeholder:text-slate-500"} focus:outline-none"></textarea>
                                </div>
                                <button type="submit" class="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-500 text-slate-950 transition hover:bg-emerald-400 focus:outline-none focus:ring-2 focus:ring-emerald-400/60 focus:ring-offset-2 ${lightTheme ? "focus:ring-offset-white" : "focus:ring-offset-slate-900"}">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3 5l18 7-18 7 3-7z" />
                                    </svg>
                                </button>
                            </div>
                            <p class="mt-3 text-xs ${lightTheme ? "text-slate-400" : "text-slate-500"}">Prototype: responses are mocked until RAG pipeline is connected.</p>
                            <div id="chat-indicator" class="htmx-indicator mt-3 flex items-center gap-2 text-xs ${lightTheme ? "text-slate-500" : "text-slate-400"}">
                                <span class="h-2 w-2 animate-pulse rounded-full bg-emerald-500"></span>
                                Sending prompt…
                            </div>
                        </form>
                    </div>
                    <aside class="flex flex-col gap-6">
                        <section class="rounded-3xl border ${lightTheme ? "border-slate-200 bg-white/80" : "border-slate-800/80 bg-slate-900/70"} p-6 shadow-xl ${lightTheme ? "shadow-emerald-100/40" : "shadow-slate-900/40"} backdrop-blur">
                            <div class="flex items-center justify-between">
                                <div>
                                    <h2 class="text-base font-semibold">build.gradle snippet</h2>
                                    <p class="text-xs ${lightTheme ? "text-slate-500" : "text-slate-400"}">Generated by the assistant. Copy or download for reuse.</p>
                                </div>
                                <div class="flex gap-2">
                                    <button type="button"
                                            class="copy-button flex h-9 w-9 items-center justify-center rounded-full border ${lightTheme ? "border-slate-200 bg-white/80 text-slate-500" : "border-slate-700/60 bg-slate-900/70 text-slate-300"} transition hover:border-emerald-400/70 hover:text-emerald-400"
                                            data-clipboard-target="#build-gradle-snippet"
                                            data-clipboard-message-target="#copy-feedback"
                                            data-clipboard-success="build.gradle snippet copied"
                                            aria-label="Copy build.gradle">
                                        <span class="sr-only">Copy build.gradle</span>
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                                            <path d="M5 15H4a2 2 0 0 1-2-2V4c0-1.1.9-2 2-2h9c1.1 0 2 .9 2 2v1" />
                                        </svg>
                                    </button>
                                    <a class="flex h-9 w-9 items-center justify-center rounded-full border ${lightTheme ? "border-slate-200 bg-white/80 text-slate-500" : "border-slate-700/60 bg-slate-900/70 text-slate-300"} transition hover:border-emerald-400/70 hover:text-emerald-400" href="/snippets/build.gradle" download>
                                        <span class="sr-only">Download build.gradle</span>
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                                            <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
                                            <path stroke-linecap="round" stroke-linejoin="round" d="M7 10l5 5 5-5" />
                                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 15V3" />
                                        </svg>
                                    </a>
                                </div>
                            </div>
                            <pre id="build-gradle-snippet" class="mt-4 max-h-[420px] overflow-y-auto rounded-2xl border ${lightTheme ? "border-slate-200 bg-slate-50 text-emerald-700" : "border-slate-800/70 bg-slate-950/80 text-emerald-200"} p-4 text-xs leading-relaxed shadow-inner ${lightTheme ? "shadow-emerald-100/40" : "shadow-black/60"}"><code>${buildGradleSnippet}</code></pre>
                            <p id="copy-feedback" class="sr-only" aria-live="polite"></p>
                            <p class="mt-2 text-[11px] ${lightTheme ? "text-slate-400" : "text-slate-500"}">Copy via the button or download the file to adapt it locally.</p>
                        </section>
                        <section class="rounded-3xl border ${lightTheme ? "border-slate-200 bg-white/80" : "border-slate-800/80 bg-slate-900/60"} p-6 shadow-xl ${lightTheme ? "shadow-emerald-100/30" : "shadow-slate-900/40"} backdrop-blur">
                            <h2 class="text-base font-semibold">How responses will be generated</h2>
                            <ol class="mt-3 list-decimal space-y-2 pl-4 text-sm ${lightTheme ? "text-slate-700" : "text-slate-300"}">
                                <li>Classify the intent (e.g. <span class="font-mono ${lightTheme ? "text-emerald-500" : "text-emerald-300"}">import INTERLIS → PostGIS</span>).</li>
                                <li>Retrieve relevant tasks, properties, and examples with BM25 + re-ranking.</li>
                                <li>Compose the answer and code with Spring AI guardrails.</li>
                            </ol>
                            <p class="mt-4 rounded-2xl border ${lightTheme ? "border-slate-200 bg-white/80 text-slate-500" : "border-slate-800/70 bg-slate-950/60 text-slate-400"} p-4 text-xs">
                                The current build simulates the pipeline so that the UI and streaming experience can be validated before wiring the live RAG components.
                            </p>
                        </section>
                    </aside>
                </div>
            </div>
        </section>
    </main>
    <footer class="border-t ${lightTheme ? "border-slate-200 bg-white/80" : "border-slate-900 bg-slate-950/80"}">
        <div class="mx-auto w-full max-w-6xl px-6 py-4 text-sm ${lightTheme ? "text-slate-500" : "text-slate-400"} flex flex-wrap items-center justify-between gap-2">
            <p>Prototype RAG assistant for GRETL documentation.</p>
            <p>Streaming powered by Spring AI (mocked).</p>
        </div>
    </footer>
</div>
</body>
</html>
