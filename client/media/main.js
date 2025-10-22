(function () {
  const vscode = acquireVsCodeApi();
  const hostElement = document.getElementById('diagram');
  if (!hostElement) {
    throw new Error('Missing diagram host element');
  }
  const clientId = hostElement.dataset.clientId ?? 'interlis-placeholder-client';
  const sourceUri = hostElement.dataset.uri ?? '';
  vscode.postMessage({ readyMessage: 'ready' });
  window.addEventListener('message', event => {
    console.debug('GLSP webview message', event.data);
  });
  console.debug('INTERLIS GLSP webview initialised', { clientId, sourceUri });
})();
//# sourceMappingURL=main.js.map
