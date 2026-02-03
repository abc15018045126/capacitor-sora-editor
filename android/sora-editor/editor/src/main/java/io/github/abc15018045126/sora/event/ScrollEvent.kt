package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor


class ScrollEvent @JvmOverloads constructor(
    editor: CodeEditor,
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val cause: Int,
    val flingVelocityX: Float = 0f,
    val flingVelocityY: Float = 0f
) : Event(editor) {

    companion object {

        const val CAUSE_USER_DRAG = 1


        const val CAUSE_USER_FLING = 2


        const val CAUSE_MAKE_POSITION_VISIBLE = 3


        const val CAUSE_TEXT_SELECTING = 4

        const val CAUSE_SCALE_TEXT = 5
    }
}
