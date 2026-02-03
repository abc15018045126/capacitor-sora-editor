package io.github.abc15018045126.sora.event

import android.view.KeyEvent
import io.github.abc15018045126.sora.widget.CodeEditor


class KeyBindingEvent(
    editor: CodeEditor,
    src: KeyEvent,
    type: Type,

    val canEditorHandle: Boolean
) : EditorKeyEvent(editor, src, type)
