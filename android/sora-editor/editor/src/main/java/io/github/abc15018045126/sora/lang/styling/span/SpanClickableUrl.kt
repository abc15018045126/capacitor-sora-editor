

package io.github.abc15018045126.sora.lang.styling.span

class SpanClickableUrl(val link: String) : SpanInteractionInfo {
    override fun isClickable() = false

    override fun isLongClickable() = false

    override fun isDoubleClickable() = true


    override fun getData(): String {
        return link
    }

}
