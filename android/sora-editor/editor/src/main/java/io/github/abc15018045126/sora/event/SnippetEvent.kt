package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor


class SnippetEvent(
    editor: CodeEditor,
    val action: Int,
    val currentTabStop: Int,
    val totalTabStop: Int
) : Event(editor) {

    companion object {

        const val ACTION_START = 1


        const val ACTION_SHIFT = 2


        const val ACTION_STOP = 3
    }
}
