

package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.lang.styling.line.LineSideIcon
import io.github.abc15018045126.sora.widget.CodeEditor


class SideIconClickEvent(editor: CodeEditor, val clickedIcon: LineSideIcon) : Event(editor) {
    override fun canIntercept(): Boolean = true
}
