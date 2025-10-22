import * as vscode from 'vscode';
import {
    GlspEditorProvider,
    GlspVscodeConnector
} from '@eclipse-glsp/vscode-integration';

const DIAGRAM_TYPE = 'interlis.uml.class';

/**
 * Custom editor provider that hosts the GLSP webview next to the text editor.
 * It delegates lifecycle hooks to {@link GlspVscodeConnector} and prepares the
 * webview HTML that loads the bundled client code.
 */
export class InterlisDiagramEditorProvider extends GlspEditorProvider {
    override diagramType = DIAGRAM_TYPE;

    constructor(
        private readonly context: vscode.ExtensionContext,
        connector: GlspVscodeConnector
    ) {
        super(connector);
    }

    override setUpWebview(
        document: vscode.CustomDocument,
        webviewPanel: vscode.WebviewPanel,
        _token: vscode.CancellationToken,
        clientId: string
    ): void {
        webviewPanel.webview.options = {
            enableScripts: true,
            localResourceRoots: [
                this.context.extensionUri,
                vscode.Uri.joinPath(this.context.extensionUri, 'media'),
                vscode.Uri.joinPath(this.context.extensionUri, 'node_modules')
            ]
        };

        const webview = webviewPanel.webview;
        const scriptUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this.context.extensionUri, 'media', 'main.js')
        );
        const styleUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this.context.extensionUri, 'media', 'main.css')
        );
        const toolkitUri = webview.asWebviewUri(
            vscode.Uri.joinPath(
                this.context.extensionUri,
                'node_modules',
                '@eclipse-glsp',
                'client',
                'glsp-client.css'
            )
        );

        const csp = [
            "default-src 'none'",
            "img-src data:",
            "style-src 'self' 'unsafe-inline'",
            "script-src 'self' 'unsafe-eval'"
        ].join('; ');

        webview.html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="${csp}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="stylesheet" href="${toolkitUri}">
  <link rel="stylesheet" href="${styleUri}">
  <title>INTERLIS UML Diagram</title>
</head>
<body>
  <div id="diagram" data-client-id="${clientId}" data-uri="${document.uri.toString()}"></div>
  <script src="${scriptUri}"></script>
</body>
</html>`;
    }
}

export const INTERLIS_DIAGRAM_TYPE = DIAGRAM_TYPE;
