

package io.github.abc15018045126.sora.graphics.inlayHint

import android.graphics.Canvas
import android.graphics.Color
import io.github.abc15018045126.sora.graphics.InlayHintRenderParams
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.lang.styling.inlayHint.ColorInlayHint
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

open class ColorInlayHintRenderer() : InlayHintRenderer() {

    companion object {
        val DefaultInstance = ColorInlayHintRenderer()
    }

    override val typeName: String
        get() = "color"

    protected val localPaint = Paint().also {
        it.isAntiAlias = true
        it.strokeWidth = 2f
    }

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        val margin = paint.spaceWidth
        return margin + params.textHeight * 0.75f
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        val centerX = measuredWidth / 2f
        val centerY = (params.textTop + params.textBottom) / 2f
        val halfSize = params.textHeight * 0.75f / 2f
        localPaint.color =
            (inlayHint as? ColorInlayHint?)?.color?.resolve(colorScheme) ?: Color.WHITE
        localPaint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
            localPaint
        )
        localPaint.color = Color.WHITE
        localPaint.style = android.graphics.Paint.Style.STROKE
        canvas.drawRect(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
            localPaint
        )
    }


}
