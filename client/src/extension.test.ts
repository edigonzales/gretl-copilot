import { beforeEach, describe, expect, it, vi } from 'vitest';

const hoisted = vi.hoisted(() => {
  const showOpenDialog = vi.fn();
  const window = {
    activeTextEditor: undefined as
      | { document: { uri: { fsPath: string; toString(): string } } }
      | undefined,
    showOpenDialog
  };
  return { window, showOpenDialog };
});

vi.mock('vscode', () => ({
  window: hoisted.window
}));

vi.mock('@eclipse-glsp/vscode-integration', () => ({
  GlspVscodeConnector: class {},
  SocketGlspVscodeServer: class {},
  GlspEditorProvider: class {}
}));

import { pickActiveIliFile } from './extension';
import type * as vscode from 'vscode';

const mockWindow = hoisted.window as {
  activeTextEditor:
    | { document: { uri: { fsPath: string; toString(): string } } }
    | undefined;
  showOpenDialog: ReturnType<typeof vi.fn>;
};
const mockShowOpenDialog = hoisted.showOpenDialog as ReturnType<typeof vi.fn>;

beforeEach(() => {
  mockWindow.activeTextEditor = undefined;
  mockShowOpenDialog.mockReset();
});

describe('pickActiveIliFile', () => {
  it('returns the URI of the active editor when it is an INTERLIS file', async () => {
    const activeUri = {
      fsPath: '/workspace/model.ili',
      toString: () => 'file:///workspace/model.ili'
    } as vscode.Uri;

    mockWindow.activeTextEditor = { document: { uri: activeUri } };

    await expect(pickActiveIliFile()).resolves.toBe(activeUri);
    expect(mockShowOpenDialog).not.toHaveBeenCalled();
  });

  it('falls back to the open dialog when no active editor is present', async () => {
    const pickedUri = {
      fsPath: '/workspace/another.ili',
      toString: () => 'file:///workspace/another.ili'
    } as vscode.Uri;
    mockShowOpenDialog.mockResolvedValueOnce([pickedUri]);

    await expect(pickActiveIliFile()).resolves.toBe(pickedUri);
    expect(mockShowOpenDialog).toHaveBeenCalledWith({
      canSelectFiles: true,
      canSelectMany: false,
      filters: { INTERLIS: ['ili'] }
    });
  });

  it('returns undefined when the picker is cancelled', async () => {
    mockShowOpenDialog.mockResolvedValueOnce(undefined);

    await expect(pickActiveIliFile()).resolves.toBeUndefined();
  });
});
