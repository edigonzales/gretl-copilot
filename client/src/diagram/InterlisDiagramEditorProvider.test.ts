import { describe, expect, it, vi } from 'vitest';

const hoisted = vi.hoisted(() => {
  const window = {
    activeTextEditor: undefined as unknown,
    showOpenDialog: vi.fn()
  };
  const joinPath = (
    ...segments: Array<{ path?: string; fsPath?: string } | string>
  ) => {
    const parts = segments.map((segment) => {
      if (typeof segment === 'string') {
        return segment;
      }
      return segment.path ?? segment.fsPath ?? '';
    });
    const path = parts.join('/');
    return {
      path,
      fsPath: path,
      toString: () => path
    };
  };
  return { window, joinPath };
});

vi.mock('vscode', () => ({
  window: hoisted.window,
  Uri: {
    joinPath: hoisted.joinPath
  }
}));

vi.mock('@eclipse-glsp/vscode-integration', () => ({
  GlspEditorProvider: class {
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    constructor(_connector: unknown) {}
  },
  GlspVscodeConnector: class {}
}));

import type * as vscode from 'vscode';
import type { GlspVscodeConnector } from '@eclipse-glsp/vscode-integration';
import { InterlisDiagramEditorProvider } from './InterlisDiagramEditorProvider';

describe('InterlisDiagramEditorProvider', () => {
  it('configures the webview HTML with the GLSP bootstrap data attributes', () => {
    const context = {
      extensionUri: { path: '/extension', fsPath: '/extension' }
    } as Pick<vscode.ExtensionContext, 'extensionUri'>;
    const connector = {} as GlspVscodeConnector;
    const provider = new InterlisDiagramEditorProvider(context, connector);

    const htmlRecorder = { value: '' };
    const webview = {
      options: undefined as vscode.WebviewOptions | undefined,
      asWebviewUri: vi.fn((uri: { path?: string; fsPath?: string }) => `mock:${uri.path ?? uri.fsPath}`),
      get html() {
        return htmlRecorder.value;
      },
      set html(value: string) {
        htmlRecorder.value = value;
      }
    };

    const panel = { webview } as unknown as vscode.WebviewPanel;
    const document = { uri: { toString: () => 'file:///workspace/model.ili' } } as vscode.CustomDocument;

    provider.setUpWebview(document, panel, {} as vscode.CancellationToken, 'client-1');

    expect(webview.options).toMatchObject({ enableScripts: true });
    expect(htmlRecorder.value).toContain('INTERLIS UML Diagram');
    expect(htmlRecorder.value).toContain('data-client-id="client-1"');
    expect(htmlRecorder.value).toContain('data-uri="file:///workspace/model.ili"');
    expect(webview.asWebviewUri).toHaveBeenCalled();
  });
});
