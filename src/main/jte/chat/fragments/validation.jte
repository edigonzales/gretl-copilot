@param String message
@param boolean lightTheme
<div hx-swap-oob="true" id="chat-alert">
    <div class="rounded-2xl border ${lightTheme ? "border-rose-400/60 bg-rose-100/50 text-rose-700" : "border-rose-500/40 bg-rose-500/10 text-rose-200"} px-4 py-3 text-sm">
        ${message}
    </div>
</div>
<span class="sr-only" aria-hidden="true"></span>
