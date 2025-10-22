import { WebviewReadyMessage } from '@eclipse-glsp/vscode-integration';

declare const acquireVsCodeApi: () => { postMessage: (message: unknown) => void };

const vscode = acquireVsCodeApi();

const hostElement = document.getElementById('diagram');
if (!hostElement) {
    throw new Error('Missing diagram host element');
}

const clientId = hostElement.dataset.clientId ?? 'interlis-placeholder-client';
const sourceUri = hostElement.dataset.uri ?? '';

const ready: WebviewReadyMessage = { readyMessage: 'ready' };
vscode.postMessage(ready);

window.addEventListener('message', event => {
    console.debug('GLSP webview message', event.data);
});

console.debug('INTERLIS GLSP webview initialised', { clientId, sourceUri });
