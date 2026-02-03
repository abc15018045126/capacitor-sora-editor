



package io.github.abc15018045126.sora.graphics.inlayHint

import android.graphics.Canvas
import io.github.abc15018045126.sora.graphics.InlayHintRenderParams
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.lang.styling.inlayHint.TextInlayHint
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


open class TextInlayHintRenderer : InlayHintRenderer() {

    companion object {
        val DefaultInstance = TextInlayHintRenderer()
    }

    protected val localPaint = Paint().also { it.isAntiAlias = true }

    override val typeName: String
        get() = "text"

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize * 0.75f

        val margin = localPaint.measureText(" ")
        val width = localPaint.measureText((inlayHint as? TextInlayHint)?.text ?: "") + margin * 2f
        return width
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        val centerY = (params.textTop + params.textBottom) / 2f
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize * 0.75f

        val margin = localPaint.measureText(" ")
        val myLineHeight = localPaint.descent() - localPaint.ascent()
        localPaint.color = colorScheme.getColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND)
        canvas.drawRoundRect(
            margin * 0.5f,
            centerY - myLineHeight / 2f,
            measuredWidth - margin * 0.5f,
            centerY + myLineHeight / 2f,
            params.textHeight * 0.15f, params.textHeight * 0.15f,
            localPaint
        )
        localPaint.color = colorScheme.getColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND)
        val myBaseline = centerY + (myLineHeight / 2f - localPaint.descent())
        canvas.drawText((inlayHint as? TextInlayHint)?.text ?: "", margin, myBaseline, localPaint)
    }

}
