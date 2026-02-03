package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.SingleCharacterWidths
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.util.BlockIntList
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.EditorTouchEventHandler
import java.util.NoSuchElementException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min


class LineBreakLayout(editor: CodeEditor, text: Content?) : AbstractLayout(editor, text) {

    private val reuseCount = AtomicInteger(0)
    private var widthMaintainer: BlockIntList? = null
    private var inlineElementsWidths: BlockIntList? = null
    private var measurer: SingleCharacterWidths? = null

    init {
        measurer = SingleCharacterWidths(editor.tabWidth).apply { isHandleFunctionCharacters = editor.isRenderFunctionCharacters }
        widthMaintainer = BlockIntList(); inlineElementsWidths = BlockIntList(); measureAllLines(widthMaintainer!!, inlineElementsWidths!!)
    }

    private fun measureAllLines(wm: BlockIntList, im: BlockIntList) {
        val text = text ?: return; val editor = editor ?: return; val p = Paint(editor.isRenderFunctionCharacters).apply { set(editor.textPaint); onAttributeUpdate() }
        val rCnt = reuseCount.get(); val m = measurer; val monitor = TaskMonitor(1, object : TaskMonitor.Callback {
            override fun onCompleted(results: Array<Any?>, cancelledCount: Int) {
                val curEditor = this@LineBreakLayout.editor ?: return; if (cancelledCount > 0) return
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (curEditor.isReleased || this@LineBreakLayout.editor !== curEditor || rCnt != reuseCount.get()) return@post
                    curEditor.setLayoutBusy(false); curEditor.touchHandler!!.scrollBy(0f, 0f)
                }
            }
        }); val task = object : LayoutTask<Void?>(monitor) {
            override fun compute(): Void? {
                wm.lock.lock(); try { text.runReadActionsOnLines(0, text.lineCount - 1, object : Content.ContentLineConsumer2 {
                    override fun accept(index: Int, line: ContentLine, flag: Content.ContentLineConsumer2.AbortFlag) {
                        val w = m?.measureText(line, 0, line.length, p)?.toInt() ?: 0; val iw = measureInlayHints(getInlayHints(index), p)
                        if (shouldRun()) { wm.add(w + iw); im.add(iw) } else flag.set = true
                    }
                }) } finally { wm.lock.unlock() }; return null
            }
            override fun shouldRun() = super.shouldRun() && reuseCount.get() == rCnt
        }; editor.setLayoutBusy(true); submitTask(task)
    }

    private fun measureInlayHints(inlayHints: List<InlayHint>, paint: Paint): Int {
        val editor = editor ?: return 0; var w = 0f
        for (h in inlayHints) editor.getInlayHintRendererForType(h.type)?.let { w += it.measure(h, paint, editor.renderer.createTextRowParams().toInlayHintRenderParams()) }
        return w.toInt()
    }

    private fun measureLineAndUpdateInlineWidths(idx: Int, useAdd: Boolean = false): Int {
        val text = text ?: return 0; val editor = editor ?: return 0; val line = text.getLine(idx); val iw = measureInlayHints(getInlayHints(idx), editor.textPaint)
        if (useAdd) inlineElementsWidths?.add(idx, iw) else inlineElementsWidths?.set(idx, iw)
        return (measurer?.measureText(line, 0, line.length, editor.textPaint)?.toInt() ?: 0) + iw
    }

    private fun measureTextRegion(idx: Int, start: Int, end: Int) = (measurer?.measureText(text?.getLine(idx), start, end, editor?.textPaint)?.toInt() ?: 0)

    override fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator = LineBreakLayoutRowItr(this, text!!, initialRow, preloadedLines)

    override fun invalidateLines(range: StyleUpdateRange) {
        val text = text ?: return; val itr = range.lineIndexIterator(text.lineCount - 1)
        while (itr.hasNext()) { val line = itr.nextInt(); widthMaintainer?.set(line, measureLineAndUpdateInlineWidths(line)) }
    }

    override val rowCount: Int
        get() = text?.lineCount ?: 0

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        val editor = this.editor ?: return
        val widthMaintainer = this.widthMaintainer ?: return
        val inlineElementsWidths = this.inlineElementsWidths ?: return

        for (i in startLine..endLine) {
            if (i == startLine) {
                if (endLine == startLine) {
                    val oldInlayWidths = inlineElementsWidths.get(i)
                    val newInlayWidths = measureInlayHints(getInlayHints(i), editor.textPaint)
                    inlineElementsWidths.set(i, newInlayWidths)
                    widthMaintainer.set(i, widthMaintainer.get(i) + measureTextRegion(i, startColumn, endColumn) + (newInlayWidths - oldInlayWidths))
                } else {
                    widthMaintainer.set(i, measureLineAndUpdateInlineWidths(i))
                }
            } else {
                widthMaintainer.add(i, measureLineAndUpdateInlineWidths(i, true))
            }
        }
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        val editor = this.editor ?: return
        val widthMaintainer = this.widthMaintainer ?: return
        val inlineElementsWidths = this.inlineElementsWidths ?: return

        if (startLine < endLine) {
            widthMaintainer.removeRange(startLine + 1, endLine + 1)
            inlineElementsWidths.removeRange(startLine + 1, endLine + 1)
        }
        if (startLine == endLine) {
            val oldInlayWidths = inlineElementsWidths.get(startLine)
            val newInlayWidths = measureInlayHints(getInlayHints(startLine), editor.textPaint)
            inlineElementsWidths.set(startLine, newInlayWidths)
            widthMaintainer.set(startLine, widthMaintainer.get(startLine)
                    - (measurer?.measureText(deletedContent, 0, endColumn - startColumn, editor.textPaint)?.toInt() ?: 0)
                    + (newInlayWidths - oldInlayWidths))
        } else {
            widthMaintainer.set(startLine, measureLineAndUpdateInlineWidths(startLine))
        }
    }

    override fun getRowAt(rowIndex: Int): Row {
        val row = Row()
        row.lineIndex = rowIndex
        row.startColumn = 0
        row.isLeadingRow = true
        row.isTrailingRow = true
        row.endColumn = text?.getColumnCount(rowIndex) ?: 0
        row.inlayHints = getInlayHints(rowIndex)
        return row
    }

    override fun getRowIndexForPosition(index: Int): Int {
        val editor = this.editor ?: return 0
        return editor.text.indexer.getCharPosition(index).line
    }

    override fun destroyLayout() {
        super.destroyLayout()
        widthMaintainer = null
        inlineElementsWidths = null
        measurer = null
    }

    override fun getLineNumberForRow(row: Int): Int {
        val lineCount = text?.lineCount ?: 1
        return max(0, min(row, lineCount - 1))
    }

    override val layoutWidth: Int
        get() {
            val widthMaintainer = this.widthMaintainer ?: return Int.MAX_VALUE / 10
            return if (widthMaintainer.size() == 0) Int.MAX_VALUE / 10 else widthMaintainer.max
        }

    override val layoutHeight: Int
        get() = (text?.lineCount ?: 0) * (editor?.logicalRowHeight ?: 0)

    override fun getRowTop(row: Int): Int {
        return row * (editor?.logicalRowHeight ?: 0)
    }

    override fun getRowBottom(row: Int): Int {
        return (row + 1) * (editor?.logicalRowHeight ?: 0)
    }

    override fun getRowIndexForY(y: Float): Int {
        val rh = editor?.logicalRowHeight ?: 1
        return (y / rh).toInt()
    }

    override fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long {
        val editor = this.editor ?: return 0
        val text = this.text ?: return 0
        val lineCount = text.lineCount
        val line = min(lineCount - 1, max((yOffset / editor.rowHeight).toInt(), 0))
        val tr = editor.renderer.createTextRow(line)
        val res = tr.getIndexForCursorOffset(xOffset)
        return IntPair.pack(line, res)
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        var dest = array
        if (dest == null || dest.size < 2) {
            dest = FloatArray(2)
        }
        val editor = this.editor ?: return dest
        dest[0] = editor.getRowBottom(line).toFloat()
        val tr = editor.renderer.createTextRow(line)
        dest[1] = tr.getCursorOffsetForIndex(column)
        return dest
    }

    override fun getRowCountForLine(line: Int): Int {
        return 1
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val text = this.text ?: return 0
        val c_line = text.lineCount
        return if (line + 1 >= c_line) {
            IntPair.pack(line, text.getColumnCount(line))
        } else {
            val c_column = text.getColumnCount(line + 1)
            val newColumn = if (column > c_column) c_column else column
            IntPair.pack(line + 1, newColumn)
        }
    }

    override fun getUpPosition(line: Int, column: Int): Long {
        val text = this.text ?: return 0
        if (line - 1 < 0) {
            return IntPair.pack(0, 0)
        }
        val c_column = text.getColumnCount(line - 1)
        val newColumn = if (column > c_column) c_column else column
        return IntPair.pack(line - 1, newColumn)
    }

    fun reuse(text: Content) {
        val editor = this.editor ?: return
        this.text = text
        reuseCount.getAndIncrement()
        measurer = SingleCharacterWidths(editor.tabWidth)
        measurer?.isHandleFunctionCharacters = editor.isRenderFunctionCharacters
        try {
            val wm = widthMaintainer
            if (wm != null && wm.lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                wm.lock.unlock()
                wm.clear()
                inlineElementsWidths?.clear()
                measureAllLines(wm, inlineElementsWidths!!)
            } else {
                widthMaintainer = BlockIntList()
                inlineElementsWidths = BlockIntList()
                measureAllLines(widthMaintainer!!, inlineElementsWidths!!)
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Unable to wait for lock", e)
        }
    }

    class LineBreakLayoutRowItr(
        private val layout: AbstractLayout,
        private val text: Content,
        private val initRow: Int,
        private val preloadedLines: SparseArray<ContentLine>?
    ) : RowIterator {

        private val result: Row = Row()
        private var currentRow: Int = initRow

        init {
            result.isLeadingRow = true
            result.isTrailingRow = true
            result.startColumn = 0
        }

        override fun next(): Row {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            result.lineIndex = currentRow
            var line = preloadedLines?.get(currentRow)
            if (line == null) {
                line = text.getLine(currentRow)
            }
            result.endColumn = line.length
            result.inlayHints = layout.getInlayHints(result.lineIndex)
            currentRow++
            return result
        }

        override fun hasNext(): Boolean {
            return currentRow >= 0 && currentRow < text.lineCount
        }

        override fun reset() {
            currentRow = initRow
        }
    }
    fun findRow(line: Int): Int {
        return line
    }
}
