
package io.github.abc15018045126.sora.widget

import android.widget.OverScroller

class EditorScroller(private val editor: CodeEditor) {

    private val scroller: OverScroller = OverScroller(editor.context)

    fun setEditorOffsets() {
        editor.scrollX = scroller.currX
        editor.scrollY = scroller.currY
    }

    @JvmOverloads
    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int = editor.getProps().scrollAnimationDurationMs) {
        scroller.startScroll(startX, startY, dx, dy, duration)
        setEditorOffsets()
    }

    fun forceFinished(finished: Boolean) {
        scroller.forceFinished(finished)
        setEditorOffsets()
    }

    fun abortAnimation() {
        scroller.abortAnimation()
        setEditorOffsets()
    }

    val isFinished: Boolean
        get() = scroller.isFinished

    val currX: Int
        get() = scroller.currX

    val currY: Int
        get() = scroller.currY

    val finalX: Int
        get() = scroller.finalX

    val finalY: Int
        get() = scroller.finalY

    val startX: Int
        get() = scroller.startX

    val startY: Int
        get() = scroller.startY

    val currVelocity: Float
        get() = scroller.currVelocity

    fun computeScrollOffset(): Boolean {
        val computed = scroller.computeScrollOffset()
        if (computed) {
            setEditorOffsets()
        }
        return computed
    }

    fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int, overX: Int, overY: Int
    ) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY)
        setEditorOffsets()
    }

    val isOverScrolled: Boolean
        get() = scroller.isOverScrolled

    val implScroller: OverScroller
        get() = scroller
}
