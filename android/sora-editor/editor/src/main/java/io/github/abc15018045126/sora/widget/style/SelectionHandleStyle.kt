package io.github.abc15018045126.sora.widget.style

import android.graphics.Canvas
import android.graphics.RectF
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


interface SelectionHandleStyle {


    fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: HandleDescriptor
    )

    fun setAlpha(alpha: Int)

    fun setScale(factor: Float)


    class HandleDescriptor {


        @JvmField
        val position = RectF()


        @JvmField
        var alignment = ALIGN_CENTER

        fun set(left: Float, top: Float, right: Float, bottom: Float, alignment: Int) {
            this.alignment = alignment
            position.set(left, top, right, bottom)
        }

        fun setEmpty() {
            position.setEmpty()
            this.alignment = ALIGN_CENTER
        }
    }

    companion object {
        const val HANDLE_TYPE_UNDEFINED = -1
        const val HANDLE_TYPE_INSERT = 0
        const val HANDLE_TYPE_LEFT = 1
        const val HANDLE_TYPE_RIGHT = 2

        const val ALIGN_CENTER = 0
        const val ALIGN_LEFT = 1
        const val ALIGN_RIGHT = 2
    }
}
