package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.span.internal.NoExtSpanImpl
import io.github.abc15018045126.sora.lang.styling.span.internal.SpanImpl


object SpanFactory {


    @JvmStatic
    fun obtain(column: Int, style: Long): Span {
        return SpanImpl.obtain(column, style)
    }


    @JvmStatic
    fun obtainNoExt(column: Int, style: Long): Span {
        return NoExtSpanImpl.obtain(column, style)
    }


    @JvmStatic
    fun recycleAll(spans: Collection<Span>) {
        for (span in spans) {
            if (!span.recycle()) {
                return
            }
        }
    }
}
