package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.TextRow
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.EditorTouchEventHandler
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


class WordwrapLayout(
    editor: CodeEditor,
    text: Content?,
    private val antiWordBreaking: Boolean,
    private val supportRtlRow: Boolean,
    oldLayout: WordwrapLayout?,
    clearCache: Boolean
) : AbstractLayout(editor, text) {

    private val width: Int
    private val miniGraphWidth: Float
    private var rowTable: MutableList<RowRegion>?

    init {
        rowTable = oldLayout?.rowTable ?: mutableListOf(); if (clearCache) rowTable?.clear()
        miniGraphWidth = if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) editor.renderer.miniGraphW else 0f
        width = (editor.width - editor.measureTextRegionOffset() - editor.extraMarginRight - miniGraphWidth * 2).toInt()
        if (width > 0 && text != null && text.lineCount > 0 && (rowTable?.isEmpty() ?: true)) {
            val limit = editor.getInitialPreviewLines()
            val linesToMeasure = if (limit <= 0) {
                val h = editor.height; val rh = editor.rowHeight; if (h > 0 && rh > 0) ceil(h.toFloat() / rh).toInt() + 10 else 50
            } else limit
            val previewLines = min(text.lineCount, linesToMeasure); val rt = rowTable ?: mutableListOf<RowRegion>().also { rowTable = it }; val tr = TextRow(); val params = editor.renderer.createTextRowParams()
            repeat(previewLines) { text.getLine(it)?.let { line -> rt.addAll(breakLine(it, line, null, tr, params)) } }; updateYOffsets(0); editor.forceSyncBreakLines = false
        }
        breakAllLines()
    }

    private fun breakAllLines() {
        val text = text ?: return; val editor = editor ?: return; if (width <= 0) { editor.setLayoutBusy(false); return }
        if (text.lineCount <= 200 && editor.getInitialPreviewLines() > 0) {
            val rt = rowTable ?: mutableListOf<RowRegion>().also { rowTable = it }; rt.clear(); val tr = TextRow(); val params = editor.renderer.createTextRowParams()
            repeat(text.lineCount) { text.getLine(it)?.let { ln -> breakLine(it, ln, null, tr, params, rt) } }; updateYOffsets(0); editor.setLayoutBusy(false); editor.touchHandler!!.scrollBy(0f, 0f); return
        }
        val taskCount = min(SUBTASK_COUNT, ceil(text.lineCount.toFloat() / MIN_LINE_COUNT_FOR_SUBTASK).toInt()); val size = text.lineCount / taskCount; val monitor = TaskMonitor(taskCount, object : TaskMonitor.Callback {
            override fun onCompleted(results: Array<Any?>, cancelledCount: Int) {
                val curEditor = this@WordwrapLayout.editor ?: return; val r2 = results.filterIsInstance<WordwrapResult>().sorted()
                io.github.abc15018045126.sora.util.EditorHandler.post {
                    if (curEditor.isReleased || this@WordwrapLayout.editor !== curEditor) return@post
                    val rt = rowTable ?: mutableListOf<RowRegion>().also { rowTable = it }; rt.clear(); r2.forEach { rt.addAll(it.regions) }; updateYOffsets(0); curEditor.setLayoutBusy(false); curEditor.touchHandler!!.scrollBy(0f, 0f)
                }
            }
        }); // editor.setLayoutBusy(true) // Removed to prevent blocking UI
        repeat(taskCount) { val start = size * it; val end = if (it + 1 == taskCount) text.lineCount - 1 else size * (it + 1) - 1; submitTask(WordwrapAnalyzeTask(monitor, it, start, end)) }
    }

    private fun findRow(line: Int): Int {
        val rt = rowTable ?: return 0; var left = 0; var right = rt.size - 1
        while (left <= right) { val mid = (left + right) / 2; val v = rt[mid].line; if (v < line) left = mid + 1 else if (v > line) right = mid - 1 else { left = mid; break } }
        var idx = min(max(0, left), rt.size - 1); if (idx < 0) return 0
        while (idx > 0 && rt[idx].startColumn > 0) idx--; return idx
    }

    fun findRow(line: Int, column: Int): Int {
        val rt = rowTable ?: return 0; var row = findRow(line)
        while (row + 1 < rt.size && rt[row].endColumn <= column && rt[row + 1].line == line) row++
        return row
    }

    private fun breakLines(startLine: Int, endLine: Int) {
        val rt = rowTable ?: return; var pos = 0
        while (pos < rt.size) { if (rt[pos].line < startLine) pos++ else break }
        while (pos < rt.size) { if (rt[pos].line in startLine..endLine) rt.removeAt(pos) else break }
        val regions = mutableListOf<RowRegion>(); val tr = TextRow(); val params = editor?.renderer?.createTextRowParams()
        for (i in startLine..endLine) text?.getLine(i)?.let { line -> breakLine(i, line, null, tr, params, regions) }
        rt.addAll(pos, regions); updateYOffsets(pos)
    }

    private fun updateYOffsets(startRow: Int) {
        val rt = rowTable ?: return; if (rt.isEmpty()) return
        var y = if (startRow > 0) rt[startRow - 1].let { it.yOffset + it.height } else 0
        for (i in startRow until rt.size) { val region = rt[i]; region.yOffset = y; y += region.height }
    }

    fun refreshHeights() {
        val rt = rowTable ?: return; val editor = editor ?: return; val lh = editor.logicalRowHeight; val wh = editor.wrapRowHeight
        for (i in rt.indices) { val region = rt[i]; val isTrailing = i == rt.size - 1 || rt[i + 1].line != region.line; region.height = if (isTrailing) lh else wh }
        updateYOffsets(0)
    }

    private fun breakLine(line: Int, seq: ContentLine, paint: Paint?, cachedTr: TextRow? = null, cachedParams: io.github.abc15018045126.sora.graphics.TextRowParams? = null, output: MutableList<RowRegion>? = null): List<RowRegion> {
        val editor = editor ?: return emptyList(); val p = paint ?: Paint(editor.isRenderFunctionCharacters).apply { set(editor.textPaint) }; val tr = cachedTr ?: TextRow()
        val dirs = text?.getLineDirections(line) ?: return emptyList(); tr.set(seq, 0, seq.length, S_SPANS_FOR_WORDWRAP, getInlayHints(line), dirs, p, null, cachedParams ?: editor.renderer.createTextRowParams())
        var isRtlBased = false; if (supportRtlRow && seq.mayNeedBidi()) { var minLevel = Int.MAX_VALUE; repeat(dirs.runCount) { minLevel = min(minLevel, dirs.getRunLevel(it)) }; if ((minLevel and 1) != 0) isRtlBased = true }
        val rows = tr.breakText(width, antiWordBreaking); val results = output ?: ArrayList(rows.size)
        for (i in rows.indices) { val row = rows[i]; val isTrailing = i == rows.size - 1; val h = if (isTrailing) editor.logicalRowHeight else editor.wrapRowHeight; results.add(RowRegion(line, row.startColumn, row.endColumn, row.inlayHints, row.rowWidth, isRtlBased, h)) }
        return results
    }

    override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent); val rt = rowTable ?: return; val delta = endLine - startLine
        if (delta != 0) for (row in findRow(startLine + 1) until rt.size) rt[row].line += delta
        breakLines(startLine, endLine)
    }

    override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent); val rt = rowTable ?: return; val delta = endLine - startLine
        if (delta != 0) { var startRow = findRow(startLine); while (startRow < rt.size) { val line = rt[startRow].line; if (line in startLine..endLine) rt.removeAt(startRow) else break }; for (row in findRow(startLine) until rt.size) { val region = rt[row]; if (region.line >= endLine) region.line -= delta } }
        breakLines(startLine, startLine)
    }

    override fun destroyLayout() { super.destroyLayout(); rowTable = null }

    override fun getRowAt(rowIndex: Int): Row {
        val rt = rowTable; if (rt == null || rt.isEmpty()) return Row().apply { lineIndex = rowIndex; isLeadingRow = true; isTrailingRow = true; endColumn = text?.getColumnCount(rowIndex) ?: 0; inlayHints = getInlayHints(rowIndex) }
        val region = rt[rowIndex]; val isLeadingRow = rowIndex <= 0 || rt[rowIndex - 1].line != region.line; val isTrailingRow = rowIndex + 1 >= rt.size || rt[rowIndex + 1].line != region.line; return region.toRow(isLeadingRow, isTrailingRow, width.toFloat())
    }

    override fun getLineNumberForRow(row: Int): Int {
        val rt = rowTable; if (rt == null || rt.isEmpty()) return max(0, min(row, (text?.lineCount ?: 1) - 1))
        return if (row >= rt.size) rt.last().line else rt[row].line
    }

    override fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator = if (rowTable.isNullOrEmpty()) LineBreakLayout.LineBreakLayoutRowItr(this, text!!, initialRow, preloadedLines) else WordwrapLayoutRowItr(initialRow)

    override fun getUpPosition(line: Int, column: Int): Long {
        val rt = rowTable; if (rt.isNullOrEmpty()) { if (line - 1 < 0) return IntPair.pack(0, 0); val cols = text?.getColumnCount(line - 1) ?: 0; return IntPair.pack(line - 1, min(column, cols)) }
        val row = findRow(line, column); if (row > 0) { val offset = column - rt[row].startColumn; val lastRow = rt[row - 1]; return IntPair.pack(lastRow.line, lastRow.startColumn + min(offset, lastRow.endColumn - lastRow.startColumn)) }
        return IntPair.pack(0, 0)
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val rt = rowTable; if (rt.isNullOrEmpty()) { val lCnt = text?.lineCount ?: 1; if (line + 1 >= lCnt) return IntPair.pack(line, text?.getColumnCount(line) ?: 0); val cols = text?.getColumnCount(line + 1) ?: 0; return IntPair.pack(line + 1, min(column, cols)) }
        val row = findRow(line, column); if (row + 1 < rt.size) { val offset = column - rt[row].startColumn; val nextRow = rt[row + 1]; return IntPair.pack(nextRow.line, nextRow.startColumn + min(offset, nextRow.endColumn - nextRow.startColumn)) }
        return IntPair.pack(line, text?.getColumnCount(line) ?: 0)
    }

    override val layoutWidth: Int get() = 0
    override val layoutHeight: Int get() {
        val rt = rowTable; val lineCount = text?.lineCount ?: 0; if (lineCount == 0 || rt.isNullOrEmpty()) return (editor?.logicalRowHeight ?: 0) * lineCount
        val last = rt.last(); if (last.line < lineCount - 1) { val pLn = last.line + 1; val curH = last.yOffset + last.height; val avgH = if (pLn > 0) curH.toDouble() / pLn else editor?.logicalRowHeight?.toDouble() ?: 0.0; return (curH + (lineCount - pLn) * avgH).toInt() }
        return last.yOffset + last.height
    }

    override fun getRowTop(row: Int): Int = rowTable?.getOrNull(row)?.yOffset ?: (row * (editor?.logicalRowHeight ?: 0))
    override fun getRowBottom(row: Int): Int = rowTable?.getOrNull(row)?.let { it.yOffset + it.height } ?: ((row + 1) * (editor?.logicalRowHeight ?: 0))

    override fun getRowIndexForY(y: Float): Int {
        val rt = rowTable; if (rt.isNullOrEmpty()) return (y / (editor?.logicalRowHeight ?: 1)).toInt()
        var left = 0; var right = rt.size - 1
        while (left <= right) { val mid = (left + right) / 2; val region = rt[mid]; if (y < region.yOffset) right = mid - 1 else if (y >= region.yOffset + region.height) left = mid + 1 else return mid }
        return max(0, min(rt.size - 1, left))
    }

    override fun getRowIndexForPosition(index: Int): Int {
        val editor = editor ?: return 0; val pos = editor.text.indexer.getCharPosition(index); val line = pos.line; val rt = rowTable ?: return line; if (rt.isEmpty()) return line
        val col = pos.column; var row = findRow(line); if (row < rt.size) { var region = rt[row]; if (region.line != line) return 0; while (region.startColumn < col && row + 1 < rt.size) { row++; region = rt[row]; if (region.line != line || region.startColumn > col) { row--; break } } ; return row }
        return 0
    }

    override fun invalidateLines(range: StyleUpdateRange) { val text = text ?: return; val itr = range.lineIndexIterator(text.lineCount - 1); while (itr.hasNext()) { val ln = itr.nextInt(); breakLines(ln, ln) } }

    override fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long {
        val editor = editor ?: return 0; val rt = rowTable; if (rt.isNullOrEmpty()) { val line = min((text?.lineCount ?: 1) - 1, max((yOffset / editor.rowHeight).toInt(), 0)); return IntPair.pack(line, editor.renderer.createTextRow(line).getIndexForCursorOffset(xOffset)) }
        var row = max(0, min(getRowIndexForY(yOffset), rt.size - 1)); val region = rt[row]; var x = xOffset; if (region.startColumn != 0) x -= miniGraphWidth
        x -= region.getRenderTranslateX(width.toFloat()); return IntPair.pack(region.line, editor.renderer.createTextRow(row).getIndexForCursorOffset(x))
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        var dest = array ?: FloatArray(2); val editor = editor ?: return dest; val rt = rowTable; if (rt.isNullOrEmpty()) { dest[0] = editor.getRowBottom(line).toFloat(); dest[1] = editor.renderer.createTextRow(line).getCursorOffsetForIndex(column); return dest }
        var row = findRow(line); if (row < rt.size) {
            var region = rt[row]; if (region.line != line) { dest[1] = 0f; dest[0] = 0f; return dest }
            while (region.startColumn < column && row + 1 < rt.size) { row++; region = rt[row]; if (region.line != line || region.startColumn > column) { row--; region = rt[row]; break } }
            dest[0] = editor.getRowBottom(row).toFloat(); dest[1] = editor.renderer.createTextRow(row).getCursorOffsetForIndex(column); if (region.startColumn != 0) dest[1] += miniGraphWidth
            dest[1] += region.getRenderTranslateX(width.toFloat())
        } else { dest[1] = 0f; dest[0] = 0f }
        return dest
    }

    override fun getRowCountForLine(line: Int): Int { val rt = rowTable ?: return 1; if (rt.isEmpty()) return 1; var row = findRow(line); var count = 0; while (row < rt.size && rt[row].line == line) { count++; row++ }; return count }

    fun getSoftBreaksForLine(line: Int): List<Int> { val rt = rowTable ?: return emptyList(); if (rt.isEmpty()) return emptyList(); var row = findRow(line); val list = mutableListOf<Int>(); while (row < rt.size && rt[row].line == line) { val column = rt[row].startColumn; if (column != 0) list.add(column); row++ }; return list }

    override val rowCount: Int get() = rowTable?.size ?: text?.lineCount ?: 0

    class RowRegion(var line: Int, val startColumn: Int, val endColumn: Int, var inlayHints: List<InlayHint>?, var rowWidth: Float, var displayFromRight: Boolean, var height: Int) {
        var yOffset: Int = 0
        fun toRow(isLeadingRow: Boolean, isTrailingRow: Boolean, layoutWidth: Float) = Row().apply { this.isLeadingRow = isLeadingRow; this.isTrailingRow = isTrailingRow; this.startColumn = this@RowRegion.startColumn; this.endColumn = this@RowRegion.endColumn; this.lineIndex = this@RowRegion.line; this.inlayHints = this@RowRegion.inlayHints ?: emptyList(); this.renderTranslateX = getRenderTranslateX(layoutWidth) }
        fun getRenderTranslateX(layoutWidth: Float) = if (displayFromRight && layoutWidth > rowWidth) layoutWidth - rowWidth else 0f
        override fun toString() = "RowRegion(startColumn=$startColumn, endColumn=$endColumn, line=$line)"
    }

    private class WordwrapResult(val index: Int, val regions: List<RowRegion>) : Comparable<WordwrapResult> { override fun compareTo(other: WordwrapResult) = index.compareTo(other.index) }

    inner class WordwrapLayoutRowItr(private val initRow: Int) : RowIterator {
        private val result = Row(); private var currentRow = initRow
        override fun next(): Row {
            val rt = rowTable ?: throw NoSuchElementException(); if (!hasNext()) throw NoSuchElementException()
            val region = rt[currentRow]; result.apply { lineIndex = region.line; startColumn = region.startColumn; endColumn = region.endColumn; inlayHints = region.inlayHints ?: emptyList(); isLeadingRow = currentRow <= 0 || rt[currentRow - 1].line != region.line; isTrailingRow = currentRow + 1 >= rt.size || rt[currentRow + 1].line != region.line; renderTranslateX = region.getRenderTranslateX(width.toFloat()) }
            currentRow++; return result
        }
        override fun hasNext() = currentRow in 0 until (rowTable?.size ?: 0)
        override fun reset() { currentRow = initRow }
    }

    private inner class WordwrapAnalyzeTask(monitor: TaskMonitor, val id: Int, val start: Int, val end: Int) : LayoutTask<WordwrapResult>(monitor) {
        private val paint = Paint(editor?.isRenderFunctionCharacters ?: false).apply { set(editor?.textPaint); onAttributeUpdate() }; private val tr = TextRow(); private val params = editor?.renderer?.createTextRowParams()
        override fun compute(): WordwrapResult { val list = mutableListOf<RowRegion>(); text?.runReadActionsOnLines(start, end, object : Content.ContentLineConsumer2 { override fun accept(index: Int, line: ContentLine, abortFlag: Content.ContentLineConsumer2.AbortFlag) { breakLine(index, line, paint, tr, params, list); if (!shouldRun()) abortFlag.set = true } }); return WordwrapResult(id, list) }
    }

    companion object { private val S_SPANS_FOR_WORDWRAP = listOf(SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 0, true, true, false))) }
}
