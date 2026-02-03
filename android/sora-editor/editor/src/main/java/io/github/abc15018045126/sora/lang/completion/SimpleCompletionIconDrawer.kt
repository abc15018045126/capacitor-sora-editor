

package io.github.abc15018045126.sora.lang.completion

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable


object SimpleCompletionIconDrawer {
    @JvmStatic
    @JvmOverloads
    fun draw(kind: CompletionItemKind, circle: Boolean = true): Drawable {
        return CircleDrawable(kind, circle)
    }
}


internal class CircleDrawable(kind: CompletionItemKind, circle: Boolean) :
    Drawable() {
    private val mPaint: Paint
    private val mTextPaint: Paint
    private val mKind: CompletionItemKind = kind
    private val mCircle: Boolean = circle

    init {
        mPaint = Paint().apply {
            isAntiAlias = true
            color = kind.defaultDisplayBackgroundColor.toInt()
        }
        mTextPaint = Paint().apply {
            color = -0x1
            isAntiAlias = true
            textSize = Resources.getSystem()
                .displayMetrics.density * 14
            textAlign = Paint.Align.CENTER
        }

    }

    override fun draw(canvas: Canvas) {
        val width = bounds.right.toFloat()
        val height = bounds.bottom.toFloat()
        if (mCircle) {
            canvas.drawCircle(width / 2, height / 2, width / 2, mPaint)
        } else {
            canvas.drawRect(0f, 0f, width, height, mPaint)
        }
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        val textCenter = -(mTextPaint.descent() + mTextPaint.ascent()) / 2f
        canvas.drawText(mKind.getDisplayChar(), 0f, textCenter, mTextPaint)
        canvas.restore()
    }

    override fun setAlpha(p1: Int) {
        mPaint.alpha = p1
        mTextPaint.alpha = p1
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mTextPaint.colorFilter = colorFilter
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.OPAQUE", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }


}
