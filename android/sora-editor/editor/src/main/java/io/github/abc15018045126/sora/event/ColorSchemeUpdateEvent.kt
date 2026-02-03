

package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor


class ColorSchemeUpdateEvent(editor: CodeEditor) : Event(editor) {

    val colorScheme
        get() = editor.colorScheme

}
