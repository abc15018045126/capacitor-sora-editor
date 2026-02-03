

package io.github.abc15018045126.sora.lang.styling.span

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor


interface SpanColorResolver : SpanExt {


    fun getForegroundColor(span: Span): ResolvableColor?


    fun getBackgroundColor(span: Span): ResolvableColor?

}
