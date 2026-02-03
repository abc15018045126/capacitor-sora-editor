package io.github.abc15018045126.sora.widget.style


interface CursorAnimator {


    fun markStartPos()


    fun markEndPos()


    fun start()


    fun cancel()


    fun isRunning(): Boolean


    fun animatedX(): Float


    fun animatedY(): Float


    fun animatedLineHeight(): Float


    fun animatedLineBottom(): Float
}
