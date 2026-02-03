package io.github.abc15018045126.sora.graphics

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.os.Build

object GraphicsCompat {


    @SuppressLint("NewApi")
    @JvmStatic
    fun drawTextRun(
        canvas: Canvas,
        text: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: android.graphics.Paint
    ) {
        canvas.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint)
    }

    @JvmStatic
    fun getRunAdvance(
        paint: Paint,
        text: CharArray,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        offset: Int
    ): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            paint.getRunAdvance(text, start, end, contextStart, contextEnd, isRtl, offset)
        } else {
            paint.measureTextRunAdvance(text, start, offset, contextStart, contextEnd, isRtl)
        }
    }
}
