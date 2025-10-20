(function () {
    function findTarget(button) {
        const selector = button.getAttribute('data-clipboard-target');
        if (!selector) {
            return null;
        }
        return button.ownerDocument.querySelector(selector);
    }

    async function copyFrom(button) {
        const target = findTarget(button);
        if (!target) {
            return { success: false, message: button.getAttribute('data-clipboard-missing') || 'Nothing to copy.' };
        }
        const text = target.innerText || target.textContent || '';
        if (!text.trim()) {
            return { success: false, message: button.getAttribute('data-clipboard-empty') || 'Nothing to copy.' };
        }
        try {
            await navigator.clipboard.writeText(text);
            return { success: true, message: button.getAttribute('data-clipboard-success') || 'Copied to clipboard.' };
        } catch (error) {
            return { success: false, message: button.getAttribute('data-clipboard-error') || 'Unable to copy.' };
        }
    }

    function dispatchMessage(button, message) {
        const targetSelector = button.getAttribute('data-clipboard-message-target');
        if (!targetSelector) {
            return;
        }
        const messageTarget = button.ownerDocument.querySelector(targetSelector);
        if (messageTarget) {
            messageTarget.textContent = message || '';
        }
    }

    function reflectState(button, success, message) {
        window.clearTimeout(button._clipboardTimeoutId);
        button.setAttribute('data-state', success ? 'copied' : 'error');
        dispatchMessage(button, message);
        button._clipboardTimeoutId = window.setTimeout(function () {
            button.removeAttribute('data-state');
            dispatchMessage(button, '');
        }, 2000);
    }

    function bindButton(button) {
        if (button._clipboardBound) {
            return;
        }
        button._clipboardBound = true;
        button.addEventListener('click', async function (event) {
            event.preventDefault();
            if (!navigator.clipboard) {
                reflectState(button, false, button.getAttribute('data-clipboard-unsupported') || 'Clipboard not available.');
                return;
            }
            const { success, message } = await copyFrom(button);
            reflectState(button, success, message);
        });
    }

    function init(root) {
        root.querySelectorAll('[data-clipboard-target]').forEach(bindButton);
    }

    if (window.htmx && typeof window.htmx.onLoad === 'function') {
        window.htmx.onLoad(init);
    } else {
        document.addEventListener('DOMContentLoaded', function () {
            init(document);
        });
    }
})();
