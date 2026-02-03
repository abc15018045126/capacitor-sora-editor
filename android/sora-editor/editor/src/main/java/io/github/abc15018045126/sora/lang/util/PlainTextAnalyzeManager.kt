package io.github.abc15018045126.sora.lang.util

import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition


class PlainTextAnalyzeManager : BaseAnalyzeManager() {

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {}

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {}

    override fun rerun() {
        val receiver = receiver
        val ref = contentRef
        if (receiver != null && ref != null) {
            val style = Styles()
            style.spans = PlainTextSpans(ref.lineCount)
            receiver.setStyles(this, style)
        } else receiver?.setStyles(this, null)
    }

}
