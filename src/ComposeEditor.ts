import { registerPlugin } from '@capacitor/core';
import { SoraEditorPlugin } from 'capacitor-sora-editor';

const ComposeEditor = registerPlugin<SoraEditorPlugin>('SoraEditor');

export default ComposeEditor;
