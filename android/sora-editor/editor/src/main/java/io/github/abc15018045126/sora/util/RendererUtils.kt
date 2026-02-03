

package io.github.abc15018045126.sora.util

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.span.SpanColorResolver
import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


object RendererUtils {


  @JvmStatic
  fun getBackgroundColor(span: Span, colorScheme: EditorColorScheme): Int {
    val resolver = span.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER)
      ?: return colorScheme.getColor(span.backgroundColorId)

    val color = resolver.getBackgroundColor(span)
      ?: return colorScheme.getColor(span.backgroundColorId)

    return color.resolve(colorScheme)
  }


  @JvmStatic
  fun getForegroundColor(span: Span, colorScheme: EditorColorScheme): Int {
    val resolver = span.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER)
      ?: return colorScheme.getColor(span.foregroundColorId)

    val color = resolver.getForegroundColor(span)
      ?: return colorScheme.getColor(span.foregroundColorId)

    return color.resolve(colorScheme)
  }
}
