

package io.github.abc15018045126.sora.widget.style.builtin

import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.style.LineNumberTipTextProvider

object DefaultLineNumberTip : LineNumberTipTextProvider {

    override fun getCurrentText(editor: CodeEditor) = "L${editor.firstVisibleLine + 1}"

}
