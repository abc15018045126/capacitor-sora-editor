package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.color.ConstColor
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.span.SpanExt


interface Span {


    var column: Int

    fun shiftColumnBy(deltaColumn: Int) {
        column += deltaColumn
    }


    var style: Long


    val foregroundColorId: Int
        get() = TextStyle.getForegroundColorId(style)


    val backgroundColorId: Int
        get() = TextStyle.getBackgroundColorId(style)


    val styleBits: Long
        get() = TextStyle.getStyleBits(style)


    fun setUnderlineColor(color: Int) {
        if (color == 0) {
            underlineColor = null
            return
        }
        underlineColor = ConstColor(color)
    }


    var underlineColor: ResolvableColor?


    var extra: Any?


    fun setSpanExt(extType: Int, ext: SpanExt?)


    fun hasSpanExt(extType: Int): Boolean


    fun <T> getSpanExt(extType: Int): T?


    fun removeAllSpanExt()


    fun reset()


    fun copy(): Span


    fun recycle(): Boolean

    companion object {

        @JvmStatic
        fun obtain(column: Int, style: Long): Span {
            return SpanFactory.obtain(column, style)
        }


        @JvmStatic
        fun recycleAll(spans: Collection<Span>) {
            SpanFactory.recycleAll(spans)
        }
    }

}
