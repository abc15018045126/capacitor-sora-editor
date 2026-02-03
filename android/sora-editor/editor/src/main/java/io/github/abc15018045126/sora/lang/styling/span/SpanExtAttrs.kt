package io.github.abc15018045126.sora.lang.styling.span

import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor

object SpanExtAttrs {

    const val EXT_COLOR_RESOLVER = 0


    const val EXT_EXTERNAL_RENDERER = 1


    const val EXT_INTERACTION_INFO = 2


    const val EXT_UNDERLINE_COLOR = 3

    @JvmStatic
    fun checkType(extType: Int, ext: SpanExt?): Boolean {
        if (ext == null) {
            return true
        }
        return when (extType) {
            EXT_COLOR_RESOLVER -> ext is SpanColorResolver
            EXT_EXTERNAL_RENDERER -> ext is SpanExternalRenderer
            EXT_INTERACTION_INFO -> ext is SpanInteractionInfo
            EXT_UNDERLINE_COLOR -> ext is ResolvableColor
            else -> true
        }
    }
}
