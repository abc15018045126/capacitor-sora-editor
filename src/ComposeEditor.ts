import { registerPlugin } from '@capacitor/core';

export interface ComposeEditorPlugin {
    openEditor(options: { filePath: string }): Promise<void>;
}

const ComposeEditor = registerPlugin<ComposeEditorPlugin>('ComposeEditor');

export default ComposeEditor;
