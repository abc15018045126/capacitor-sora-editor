package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.CodeEditor


class ContentChangeEvent(
    editor: CodeEditor,
    val action: Int,
    val changeStart: CharPosition,
    val changeEnd: CharPosition,
    val changedText: CharSequence,
    val isCausedByUndoManager: Boolean
) : Event(editor) {

    companion object {

        const val ACTION_SET_NEW_TEXT = 1

        const val ACTION_INSERT = 2

        const val ACTION_DELETE = 3
    }
}
