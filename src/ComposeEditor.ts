import { registerPlugin } from '@capacitor/core';

export interface ComposeEditorPlugin {
    openEditor(options: { filePath: string; autoFocus?: boolean }): Promise<void>;
}

const ComposeEditor = registerPlugin<ComposeEditorPlugin>('SoraEditor');

export default ComposeEditor;
