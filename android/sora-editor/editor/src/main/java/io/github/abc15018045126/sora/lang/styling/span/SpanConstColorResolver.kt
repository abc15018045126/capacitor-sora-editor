

package io.github.abc15018045126.sora.lang.styling.span

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.color.ConstColor


class SpanConstColorResolver(foreground: Int = 0, background: Int = 0) : SpanColorResolver {

    private val foregroundColor = if (foreground == 0) null else ConstColor(foreground)

    private val backgroundColor = if (background == 0) null else ConstColor(background)

    override fun getForegroundColor(span: Span) = foregroundColor

    override fun getBackgroundColor(span: Span) = backgroundColor


}
