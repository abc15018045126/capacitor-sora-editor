

package io.github.abc15018045126.sora.graphics

import android.graphics.Paint
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

data class TextRowParams(
    val tabWidth: Int,
    val textMetrics: Paint.FontMetricsInt,
    val textTop: Int,
    val textBottom: Int,
    val textHeight: Int,
    val textBaseline: Int,
    val rowTop: Int,
    val rowBottom: Int,
    val rowHeight: Int,
    val roundTextBackgroundFactor: Float,
    val inlayHintRendererProvider: InlayHintRendererProvider,
    val colorScheme: EditorColorScheme,
    val miscPaint: Paint,
    val graphPaint: Paint,
    val graphMetrics: Paint.FontMetricsInt
) {
    fun toInlayHintRenderParams() = InlayHintRenderParams(
        tabWidth,
        textMetrics,
        textTop,
        textBottom,
        textHeight,
        textBaseline,
        rowTop,
        rowBottom,
        rowHeight,
        roundTextBackgroundFactor
    )
}

data class InlayHintRenderParams(
    val tabWidth: Int,
    val textMetrics: Paint.FontMetricsInt,
    val textTop: Int,
    val textBottom: Int,
    val textHeight: Int,
    val textBaseline: Int,
    val rowTop: Int,
    val rowBottom: Int,
    val rowHeight: Int,
    val roundTextBackgroundFactor: Float
)
