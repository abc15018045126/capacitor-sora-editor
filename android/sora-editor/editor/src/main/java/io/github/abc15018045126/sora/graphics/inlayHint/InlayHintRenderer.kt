



package io.github.abc15018045126.sora.graphics.inlayHint

import android.graphics.Canvas
import io.github.abc15018045126.sora.graphics.InlayHintRenderParams
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


abstract class InlayHintRenderer() {


    abstract val typeName: String

    fun measure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float = onMeasure(inlayHint, paint, params)

    fun render(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) = onRender(
        inlayHint,
        canvas,
        paint,
        params,
        colorScheme,
        measuredWidth
    )


    abstract fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float


    abstract fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    )

}
