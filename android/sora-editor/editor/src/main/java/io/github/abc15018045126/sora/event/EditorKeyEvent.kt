package io.github.abc15018045126.sora.event

import android.view.KeyEvent
import io.github.abc15018045126.sora.widget.CodeEditor

open class EditorKeyEvent(
    editor: CodeEditor,
    private val src: KeyEvent,
    val eventType: Type
) : ResultedEvent<Boolean>(editor) {

    private val shiftPressed: Boolean = editor.getKeyMetaStates().isShiftPressed
    private val altPressed: Boolean = editor.getKeyMetaStates().isAltPressed

    override fun canIntercept(): Boolean {
        return true
    }

    val action: Int
        get() = src.action

    val keyCode: Int
        get() = src.keyCode

    val repeatCount: Int
        get() = src.repeatCount

    val metaState: Int
        get() = src.metaState

    val modifiers: Int
        get() = src.modifiers

    val downTime: Long
        get() = src.downTime

    override val eventTime: Long
        get() = src.eventTime

    fun isShiftPressed(): Boolean {
        return shiftPressed
    }

    fun isAltPressed(): Boolean {
        return altPressed
    }

    fun isCtrlPressed(): Boolean {
        return (src.metaState and KeyEvent.META_CTRL_ON) != 0
    }

    fun markAsConsumed() {
        interceptAndSetResult(true)
    }

    fun result(editorResult: Boolean): Boolean {
        val res = result
        val userResult = res ?: false
        return if (isIntercepted()) {
            userResult
        } else {
            userResult || editorResult
        }
    }


    enum class Type {

        UP,


        DOWN,

        MULTIPLE
    }
}
