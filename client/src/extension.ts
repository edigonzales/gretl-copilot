import * as vscode from 'vscode';
import {
    GlspVscodeConnector,
    SocketGlspVscodeServer
} from '@eclipse-glsp/vscode-integration';
import { InterlisDiagramEditorProvider } from './diagram/InterlisDiagramEditorProvider';

const CLIENT_ID = 'interlis-glsp-client';
const CLIENT_NAME = 'INTERLIS GLSP VSCode';
const DEFAULT_PORT = 7012;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
    const server = new SocketGlspVscodeServer({
        clientId: CLIENT_ID,
        clientName: CLIENT_NAME,
        connectionOptions: {
            host: '127.0.0.1',
            port: DEFAULT_PORT
        }
    });

    await server.start();

    const connector = new GlspVscodeConnector({ server });
    const editorProvider = new InterlisDiagramEditorProvider(context, connector);

    context.subscriptions.push(
        vscode.window.registerCustomEditorProvider('interlis.glsp.diagram', editorProvider, {
            webviewOptions: {
                retainContextWhenHidden: true
            }
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('interlis.openGlspDiagram', async (uri?: vscode.Uri) => {
            const targetUri = uri ?? (await pickActiveIliFile());
            if (!targetUri) {
                vscode.window.showWarningMessage('Open an INTERLIS (*.ili) file before launching the diagram view.');
                return;
            }

            await vscode.commands.executeCommand(
                'vscode.openWith',
                targetUri,
                'interlis.glsp.diagram',
                {
                    preview: false,
                    viewColumn: vscode.ViewColumn.Beside
                }
            );
        })
    );
}

export function deactivate(): void {
    // Nothing to clean up explicitly; disposables handle the lifecycle.
}

export async function pickActiveIliFile(): Promise<vscode.Uri | undefined> {
    const activeEditor = vscode.window.activeTextEditor;
    if (activeEditor && activeEditor.document.uri.fsPath.endsWith('.ili')) {
        return activeEditor.document.uri;
    }

    const pick = await vscode.window.showOpenDialog({
        canSelectFiles: true,
        canSelectMany: false,
        filters: { INTERLIS: ['ili'] }
    });
    return pick?.[0];
}
