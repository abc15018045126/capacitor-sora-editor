package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.EditorSearcher


class SelectionChangeEvent(
    editor: CodeEditor,
    val oldLeft: CharPosition?,
    val oldRight: CharPosition?,
    val cause: Int
) : Event(editor) {

    val left: CharPosition = editor.text.cursor.left()
    val right: CharPosition = editor.text.cursor.right()


    val isSelected: Boolean
        get() = left.index != right.index

    companion object {

        const val CAUSE_UNKNOWN = 0


        const val CAUSE_TEXT_MODIFICATION = 1


        const val CAUSE_SELECTION_HANDLE = 2


        const val CAUSE_TAP = 3


        const val CAUSE_IME = 4


        const val CAUSE_LONG_PRESS = 5


        const val CAUSE_SEARCH = 6


        const val CAUSE_KEYBOARD_OR_CODE = 7


        const val CAUSE_MOUSE_INPUT = 8


        const val CAUSE_DEAD_KEYS = 9
    }
}
