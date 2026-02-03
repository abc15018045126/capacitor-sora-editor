package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.event.EventReceiver
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.event.Unsubscribe

internal class CursorBlink(val editor: CodeEditor, p: Int) : Runnable, EventReceiver<SelectionChangeEvent> {
    @JvmField var visibility = true
    @JvmField var valid = true
    @JvmField var lastSelectionModificationTime = 0L
    @JvmField var period = p
    private var buffer: FloatArray? = null

    init { editor.subscribeEvent(SelectionChangeEvent::class.java, this) }

    override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) = onSelectionChanged()

    fun setPeriod(p: Int) {
        period = p
        valid = p > 0
        if (!valid) visibility = true
    }

    fun onSelectionChanged() {
        lastSelectionModificationTime = System.currentTimeMillis()
        visibility = true
    }

    fun isSelectionVisible(): Boolean {
        val buf = buffer ?: return false
        return buf[0] >= editor.offsetY && buf[0] - editor.rowHeight <= editor.offsetY + editor.height &&
               buf[1] >= editor.offsetX && buf[1] - 100f <= editor.offsetX + editor.width
    }

    override fun run() {
        if (valid && period > 0) {
            if (System.currentTimeMillis() - lastSelectionModificationTime >= period * 2L) {
                visibility = !visibility
                val c = editor.cursor
                if (c != null) {
                    val left = c.left()
                    buffer = editor.layout?.getCharLayoutOffset(left.line, left.column, buffer)
                    if (!c.isSelected && isSelectionVisible()) editor.postInvalidate()
                }
            } else visibility = true
            editor.postDelayedInLifecycle(this, period.toLong())
        } else visibility = true
    }
}
