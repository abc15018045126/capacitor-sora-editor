package io.github.abc15018045126.sora.widget

import android.widget.OverScroller
import androidx.annotation.NonNull

class EditorScroller(@NonNull private val editor: CodeEditor) {
    private val scroller: OverScroller = OverScroller(editor.context)

    fun setEditorOffsets() { editor.scrollX = scroller.currX; editor.scrollY = scroller.currY }
    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) = startScroll(startX, startY, dx, dy, editor.props!!.scrollAnimationDurationMs)
    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) { scroller.startScroll(startX, startY, dx, dy, duration); setEditorOffsets() }
    fun forceFinished(finished: Boolean) { scroller.forceFinished(finished); setEditorOffsets() }
    fun abortAnimation() { scroller.abortAnimation(); setEditorOffsets() }

    val isFinished get() = scroller.isFinished
    fun getCurrX() = scroller.currX
    fun getCurrY() = scroller.currY
    fun getFinalX() = scroller.finalX
    fun getFinalY() = scroller.finalY
    fun getStartX() = scroller.startX
    fun getStartY() = scroller.startY
    fun getCurrVelocity() = scroller.currVelocity

    fun computeScrollOffset(): Boolean { val computed = scroller.computeScrollOffset(); if (computed) setEditorOffsets(); return computed }
    fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int, overX: Int, overY: Int) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY); setEditorOffsets()
    }
    fun isOverScrolled() = scroller.isOverScrolled
    fun getImplScroller() = scroller
}
