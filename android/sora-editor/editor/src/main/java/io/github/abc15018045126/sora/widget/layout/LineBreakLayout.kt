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
        val rCnt = reuseCount.get(); val m = measurer ?: return; val monitor = TaskMonitor(1, object : TaskMonitor.Callback {
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
                        val w = m.measureText(line, 0, line.length, p).toInt(); val iw = measureInlayHints(getInlayHints(index), p)
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
        val text = text ?: return 0; val editor = editor ?: return 0; val line = text.getLine(idx); val paint = editor.textPaint; val iw = measureInlayHints(getInlayHints(idx), paint)
        if (useAdd) inlineElementsWidths?.add(idx, iw) else inlineElementsWidths?.set(idx, iw)
        return (measurer?.measureText(line, 0, line.length, paint)?.toInt() ?: 0) + iw
    }

    private fun measureTextRegion(idx: Int, start: Int, end: Int): Int {
        val editor = editor ?: return 0; val paint = editor.textPaint; val m = measurer ?: return 0
        return text?.getLine(idx)?.let { m.measureText(it, start, end, paint).toInt() } ?: 0
    }

    override fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator = LineBreakLayoutRowItr(this, text!!, initialRow, preloadedLines)

    override fun invalidateLines(range: StyleUpdateRange) {
        val text = text ?: return; val itr = range.lineIndexIterator(text.lineCount - 1)
        while (itr.hasNext()) { val line = itr.nextInt(); widthMaintainer?.set(line, measureLineAndUpdateInlineWidths(line)) }
    }

    override val rowCount: Int
        get() = text?.lineCount ?: 0

    override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent); val editor = editor ?: return; val wm = widthMaintainer ?: return; val im = inlineElementsWidths ?: return
        for (i in startLine..endLine) {
            if (i == startLine) {
                if (endLine == startLine) { val old = im.get(i); val new = measureInlayHints(getInlayHints(i), editor.textPaint); im.set(i, new); wm.set(i, wm.get(i) + measureTextRegion(i, startColumn, endColumn) + (new - old)) }
                else wm.set(i, measureLineAndUpdateInlineWidths(i))
            } else wm.add(i, measureLineAndUpdateInlineWidths(i, true))
        }
    }

    override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent); val editor = editor ?: return; val wm = widthMaintainer ?: return; val im = inlineElementsWidths ?: return
        if (startLine < endLine) { wm.removeRange(startLine + 1, endLine + 1); im.removeRange(startLine + 1, endLine + 1) }
        if (startLine == endLine) { val old = im.get(startLine); val new = measureInlayHints(getInlayHints(startLine), editor.textPaint); im.set(startLine, new); val m = measurer ?: return; wm.set(startLine, wm.get(startLine) - m.measureText(deletedContent, 0, endColumn - startColumn, editor.textPaint).toInt() + (new - old)) }
        else wm.set(startLine, measureLineAndUpdateInlineWidths(startLine))
    }

    override fun getRowAt(rowIndex: Int) = Row().apply { lineIndex = rowIndex; startColumn = 0; isLeadingRow = true; isTrailingRow = true; endColumn = text?.getColumnCount(rowIndex) ?: 0; inlayHints = getInlayHints(rowIndex) }
    override fun getRowIndexForPosition(index: Int) = editor?.text?.indexer?.getCharPosition(index)?.line ?: 0
    override fun destroyLayout() { super.destroyLayout(); widthMaintainer = null; inlineElementsWidths = null; measurer = null }
    override fun getLineNumberForRow(row: Int) = max(0, min(row, (text?.lineCount ?: 1) - 1))
    override val layoutWidth: Int get() = widthMaintainer?.let { if (it.size() == 0) Int.MAX_VALUE / 10 else it.max } ?: (Int.MAX_VALUE / 10)
    override val layoutHeight: Int get() = (text?.lineCount ?: 0) * (editor?.logicalRowHeight ?: 0)
    override fun getRowTop(row: Int) = row * (editor?.logicalRowHeight ?: 0)
    override fun getRowBottom(row: Int) = (row + 1) * (editor?.logicalRowHeight ?: 0)
    override fun getRowIndexForY(y: Float) = (y / (editor?.logicalRowHeight ?: 1)).toInt()

    override fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long {
        val editor = editor ?: return 0; val line = min((text?.lineCount ?: 1) - 1, max((yOffset / editor.rowHeight).toInt(), 0))
        return IntPair.pack(line, editor.renderer.createTextRow(line).getIndexForCursorOffset(xOffset))
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        val dest = array ?: FloatArray(2); val editor = editor ?: return dest; dest[0] = editor.getRowBottom(line).toFloat(); dest[1] = editor.renderer.createTextRow(line).getCursorOffsetForIndex(column); return dest
    }

    override fun getRowCountForLine(line: Int) = 1
    override fun getDownPosition(line: Int, column: Int): Long { val text = text ?: return 0; return if (line + 1 >= text.lineCount) IntPair.pack(line, text.getColumnCount(line)) else { val cols = text.getColumnCount(line + 1); IntPair.pack(line + 1, min(column, cols)) } }
    override fun getUpPosition(line: Int, column: Int): Long { val text = text ?: return 0; if (line - 1 < 0) return IntPair.pack(0, 0); val cols = text.getColumnCount(line - 1); return IntPair.pack(line - 1, min(column, cols)) }

    fun reuse(text: Content) {
        val editor = editor ?: return; this.text = text; reuseCount.getAndIncrement(); measurer = SingleCharacterWidths(editor.tabWidth).apply { isHandleFunctionCharacters = editor.isRenderFunctionCharacters }
        try { val wm = widthMaintainer; if (wm != null && wm.lock.tryLock(5, TimeUnit.MILLISECONDS)) { wm.lock.unlock(); wm.clear(); inlineElementsWidths?.clear(); measureAllLines(wm, inlineElementsWidths!!) } else { widthMaintainer = BlockIntList(); inlineElementsWidths = BlockIntList(); measureAllLines(widthMaintainer!!, inlineElementsWidths!!) } }
        catch (e: InterruptedException) { throw RuntimeException("Unable to wait for lock", e) }
    }

    class LineBreakLayoutRowItr(private val layout: AbstractLayout, private val text: Content, private val initRow: Int, private val preloadedLines: SparseArray<ContentLine>?) : RowIterator {
        private val result = Row().apply { isLeadingRow = true; isTrailingRow = true; startColumn = 0 }; private var currentRow = initRow
        override fun next(): Row { if (!hasNext()) throw NoSuchElementException(); result.lineIndex = currentRow; val line = preloadedLines?.get(currentRow) ?: text.getLine(currentRow); result.endColumn = line.length; result.inlayHints = layout.getInlayHints(result.lineIndex); currentRow++; return result }
        override fun hasNext() = currentRow in 0 until text.lineCount
        override fun reset() { currentRow = initRow }
    }
    fun findRow(line: Int) = line
}
