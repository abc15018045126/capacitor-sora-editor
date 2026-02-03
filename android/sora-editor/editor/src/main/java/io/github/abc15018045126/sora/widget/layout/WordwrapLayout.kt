package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.TextRow
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor
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
    
    // Block-based Virtualization
    private val blocks = ArrayList<Block>()
    private var cachedRowCount = 0
    private var cachedTotalHeight = 0
    private val BLOCK_SIZE = 500

    init {
        miniGraphWidth = if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) editor.renderer.miniGraphW else 0f
        width = (editor.width - editor.measureTextRegionOffset() - editor.extraMarginRight - miniGraphWidth * 2).toInt()
        
        initBlocks()
    }

    private fun initBlocks() {
        blocks.clear()
        val text = text ?: return
        val totalLines = text.lineCount
        var startLine = 0
        while (startLine < totalLines) {
            val count = min(BLOCK_SIZE, totalLines - startLine)
            blocks.add(Block(startLine, count, editor!!.logicalRowHeight))
            startLine += count
        }
        updateBlockOffsets()
        
    }

    private fun updateBlockOffsets() {
        var r = 0; var y = 0
        for (b in blocks) {
            b.startRow = r; b.yOffset = y
            r += b.rowCount; y += b.height
        }
        cachedRowCount = r
        cachedTotalHeight = y
    }

    private fun measureBlock(b: Block) {
        if (b.isMeasured) return
        val text = text ?: return
        // Safety check if text lines changed before re-init
        if (b.startLine >= text.lineCount) return 

        val regions = ArrayList<RowRegion>(b.originalLineCount)
        val tr = TextRow(); val params = editor?.renderer?.createTextRowParams()
        
        for (i in 0 until b.originalLineCount) {
            val lineIndex = b.startLine + i
            if (lineIndex >= text.lineCount) break
            text.getLine(lineIndex)?.let { line ->
                 breakLine(lineIndex, line, null, tr, params, regions)
            }
        }
        b.regions = regions
        b.rowCount = regions.size
        b.height = regions.sumOf { it.height }
        b.isMeasured = true
        updateBlockOffsets()
    }
    
    private fun findBlockByRow(row: Int): Block? {
        if (blocks.isEmpty()) return null
        var left = 0; var right = blocks.size - 1
        while (left <= right) {
            val mid = (left + right) / 2
            val b = blocks[mid]
            if (row < b.startRow) right = mid - 1
            else if (row >= b.startRow + b.rowCount) left = mid + 1
            else return b
        }
        return null
    }

    private fun findBlockByLine(line: Int): Block? {
        if (blocks.isEmpty()) return null
        val index = line / BLOCK_SIZE
        return if (index in blocks.indices) blocks[index] else null
    }

    fun findRow(line: Int, column: Int): Int {
         val b = findBlockByLine(line) ?: return 0
         if (!b.isMeasured) measureBlock(b)
         val list = b.regions ?: return b.startRow + (line - b.startLine)
         
         var left = 0; var right = list.size - 1
         var foundRow = 0
         while (left <= right) {
             val mid = (left + right) / 2
             val r = list[mid]
             if (r.line < line) left = mid + 1
             else if (r.line > line) right = mid - 1
             else {
                 if (column < r.startColumn) right = mid - 1 
                 else if (column >= r.endColumn && mid + 1 < list.size && list[mid+1].line == line) left = mid + 1
                 else { foundRow = mid; break }
             }
         }
         if (foundRow == 0 && list.isNotEmpty() && list.last().line < line) foundRow = list.size - 1
         return b.startRow + foundRow
    }
    
    // Helper for internal use (Layout interface mostly uses row index)
    // But AbstractLayout might call findRow(line)
    private fun findRow(line: Int): Int {
        val b = findBlockByLine(line) ?: return 0
        if (!b.isMeasured) return b.startRow + (line - b.startLine)
        
        val list = b.regions ?: return b.startRow
        var left = 0; var right = list.size - 1
        var result = -1
        while (left <= right) {
            val mid = (left + right) / 2
            if (list[mid].line >= line) {
                if (list[mid].line == line) result = mid
                right = mid - 1
            } else {
                left = mid + 1
            }
        }
        return if (result != -1) b.startRow + result else b.startRow
    }

    fun refreshHeights() {
        blocks.forEach { it.isMeasured = false } // Invalidate all
        blocks.firstOrNull()?.let { measureBlock(it) }
        updateBlockOffsets()
    }

    fun updateRowHeights() {
        val editor = editor ?: return
        val newHeight = editor.logicalRowHeight
        blocks.forEach { b ->
            if (!b.isMeasured) {
                b.height = b.originalLineCount * newHeight
            } else {
                b.regions?.forEach { 
                    it.height = newHeight 
                }
                b.height = (b.regions?.sumOf { it.height } ?: (b.rowCount * newHeight))
            }
        }
        updateBlockOffsets()
    }

    private fun breakLine(line: Int, seq: ContentLine, paint: Paint?, cachedTr: TextRow? = null, cachedParams: io.github.abc15018045126.sora.graphics.TextRowParams? = null, output: MutableList<RowRegion>? = null): List<RowRegion> {
        val editor = editor ?: return emptyList(); val p = paint ?: Paint(editor.isRenderFunctionCharacters).apply { set(editor.textPaint) }; val tr = cachedTr ?: TextRow()
        
        // Fast Path
        val hints = getInlayHints(line)
        if ((!supportRtlRow || !seq.mayNeedBidi()) && hints.isEmpty() && seq.length < 4096) {
            var hasTab = false; var i = 0
            while (i < seq.length) { if (seq[i] == '\t') { hasTab = true; break }; i++ }
            if (!hasTab) {
                val w = p.measureText(seq, 0, seq.length)
                if (w <= width) {
                    val results = output ?: ArrayList(1)
                    results.add(RowRegion(line, 0, seq.length, null, w, false, editor.logicalRowHeight))
                    return results
                }
            }
        }

        val dirs = text?.getLineDirections(line) ?: return emptyList(); tr.set(seq, 0, seq.length, S_SPANS_FOR_WORDWRAP, hints, dirs, p, null, cachedParams ?: editor.renderer.createTextRowParams())
        var isRtlBased = false; if (supportRtlRow && seq.mayNeedBidi()) { var minLevel = Int.MAX_VALUE; repeat(dirs.runCount) { minLevel = min(minLevel, dirs.getRunLevel(it)) }; if ((minLevel and 1) != 0) isRtlBased = true }
        val rows = tr.breakText(width, antiWordBreaking); val results = output ?: ArrayList(rows.size)
        for (i in rows.indices) { val row = rows[i]; val isTrailing = i == rows.size - 1; val h = if (isTrailing) editor.logicalRowHeight else editor.wrapRowHeight; results.add(RowRegion(line, row.startColumn, row.endColumn, row.inlayHints, row.rowWidth, isRtlBased, h)) }
        return results
    }

    override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        initBlocks() 
    }

    override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        initBlocks()
    }

    override fun destroyLayout() { super.destroyLayout(); blocks.clear() }

    override fun getRowAt(rowIndex: Int): Row {
        val b = findBlockByRow(rowIndex) 
        if (b == null) return Row().apply { lineIndex = 0; startColumn = 0; endColumn = 0 }
        
        if (!b.isMeasured) measureBlock(b)
        
        val regions = b.regions
        if (regions.isNullOrEmpty()) {
             val line = b.startLine + (rowIndex - b.startRow)
             return Row().apply { lineIndex = line; startColumn = 0; endColumn = text?.getColumnCount(line)?:0 }
        }
        
        val localIndex = rowIndex - b.startRow
        if (localIndex in regions.indices) {
            val region = regions[localIndex]
             val isLeadingRow = localIndex <= 0 || regions[localIndex - 1].line != region.line; 
             val isTrailingRow = localIndex + 1 >= regions.size || regions[localIndex + 1].line != region.line;
            return region.toRow(isLeadingRow, isTrailingRow, width.toFloat())
        }
        return Row()
    }

    override fun getLineNumberForRow(row: Int): Int {
         val b = findBlockByRow(row) ?: return 0
         if (!b.isMeasured) return b.startLine + (row - b.startRow)
         val regions = b.regions ?: return b.startLine
         val local = row - b.startRow
         return if (local in regions.indices) regions[local].line else b.startLine
    }

    override fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator = WordwrapLayoutRowItr(initialRow)

    override fun getUpPosition(line: Int, column: Int): Long {
        val row = findRow(line, column)
        if (row <= 0) return IntPair.pack(0, 0)
        
        val prevRowIdx = row - 1
        val prevRow = getRowAt(prevRowIdx) 
        val currRow = getRowAt(row)
        
        val offset = column - currRow.startColumn
        return IntPair.pack(prevRow.lineIndex, prevRow.startColumn + min(offset, prevRow.endColumn - prevRow.startColumn))
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val row = findRow(line, column)
        if (row + 1 >= rowCount) return IntPair.pack(line, text?.getColumnCount(line)?:0)
        
        val nextRowIdx = row + 1
        val nextRow = getRowAt(nextRowIdx)
        val currRow = getRowAt(row)
        
        val offset = column - currRow.startColumn
        return IntPair.pack(nextRow.lineIndex, nextRow.startColumn + min(offset, nextRow.endColumn - nextRow.startColumn))
    }

    override val layoutWidth: Int get() = 0
    override val layoutHeight: Int get() = cachedTotalHeight

    override fun getRowTop(row: Int): Int {
        val b = findBlockByRow(row) ?: return row * (editor?.logicalRowHeight ?: 0)
        if (!b.isMeasured) {
             return b.yOffset + (row - b.startRow) * (editor?.logicalRowHeight ?: 0)
        }
        val local = row - b.startRow
        var y = b.yOffset
        val regions = b.regions ?: return y
        for (i in 0 until min(local, regions.size)) y += regions[i].height
        return y
    }

    override fun getRowBottom(row: Int): Int {
        val b = findBlockByRow(row) ?: return (row + 1) * (editor?.logicalRowHeight ?: 0)
        val h = if (b.isMeasured && b.regions != null && (row - b.startRow) in b.regions!!.indices) b.regions!![row - b.startRow].height else editor?.logicalRowHeight ?: 0
        return getRowTop(row) + h
    }

    override fun getRowIndexForY(y: Float): Int {
        val iy = y.toInt()
        var left = 0; var right = blocks.size - 1
        var bestB: Block? = null
        while (left <= right) {
            val mid = (left + right) / 2
            val b = blocks[mid]
            if (iy < b.yOffset) right = mid - 1
            else if (iy >= b.yOffset + b.height) left = mid + 1
            else { bestB = b; break }
        }
        val b = bestB ?: blocks.lastOrNull() ?: return 0
        
        if (!b.isMeasured) {
            return b.startRow + (iy - b.yOffset) / (editor?.logicalRowHeight ?: 1)
        }
        
        var curY = b.yOffset
        val regions = b.regions ?: return b.startRow
        for (i in regions.indices) {
            val h = regions[i].height
            if (iy < curY + h) return b.startRow + i
            curY += h
        }
        return b.startRow + b.rowCount - 1
    }

    override fun getRowIndexForPosition(index: Int): Int {
        val editor = editor ?: return 0
        val pos = editor.text.indexer.getCharPosition(index)
        return findRow(pos.line, pos.column)
    }

    override fun invalidateLines(range: StyleUpdateRange) { 
        val tm = text ?: return
        val itr = range.lineIndexIterator(tm.lineCount - 1)
        while (itr.hasNext()) {
            val line = itr.nextInt()
            findBlockByLine(line)?.isMeasured = false
        }
        updateBlockOffsets()
    }

    override fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long {
        val rowIdx = getRowIndexForY(yOffset)
        val row = getRowAt(rowIdx) 
        val line = row.lineIndex
        
        val b = findBlockByRow(rowIdx)
        if (b != null && b.isMeasured && b.regions != null) {
            val local = rowIdx - b.startRow
            if (local in b.regions!!.indices) {
                val region = b.regions!![local]
                var x = xOffset; if (region.startColumn != 0) x -= miniGraphWidth
                x -= region.getRenderTranslateX(width.toFloat())
                return IntPair.pack(line, editor!!.renderer.createTextRow(line).getIndexForCursorOffset(x))
            }
        }
        return IntPair.pack(line, editor!!.renderer.createTextRow(line).getIndexForCursorOffset(xOffset))
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        var dest = array ?: FloatArray(2); val editor = editor ?: return dest
        
        val rowIdx = findRow(line, column)
        val b = findBlockByRow(rowIdx)
        
        dest[0] = getRowBottom(rowIdx).toFloat() 
        
        if (b != null && b.isMeasured && b.regions != null) {
             val local = rowIdx - b.startRow
             if (local in b.regions!!.indices) {
                 val region = b.regions!![local]
                 dest[1] = editor.renderer.createTextRow(line).getCursorOffsetForIndex(column)
                 if (region.startColumn != 0) dest[1] += miniGraphWidth
                 dest[1] += region.getRenderTranslateX(width.toFloat())
                 return dest
             }
        }
        
        dest[1] = editor.renderer.createTextRow(line).getCursorOffsetForIndex(column)
        return dest
    }

    override fun getRowCountForLine(line: Int): Int { 
        val b = findBlockByLine(line) ?: return 1
        if (!b.isMeasured) return 1
        return b.regions?.count { it.line == line } ?: 1
    }

    fun getSoftBreaksForLine(line: Int): List<Int> { 
        val b = findBlockByLine(line) ?: return emptyList()
        if (!b.isMeasured) measureBlock(b)
        return b.regions?.filter { it.line == line && it.startColumn != 0 }?.map { it.startColumn } ?: emptyList()
    }

    override val rowCount: Int get() = cachedRowCount

    class RowRegion(var line: Int, val startColumn: Int, val endColumn: Int, var inlayHints: List<InlayHint>?, var rowWidth: Float, var displayFromRight: Boolean, var height: Int) {
        fun toRow(isLeadingRow: Boolean, isTrailingRow: Boolean, layoutWidth: Float) = Row().apply { this.isLeadingRow = isLeadingRow; this.isTrailingRow = isTrailingRow; this.startColumn = this@RowRegion.startColumn; this.endColumn = this@RowRegion.endColumn; this.lineIndex = this@RowRegion.line; this.inlayHints = this@RowRegion.inlayHints ?: emptyList(); this.renderTranslateX = getRenderTranslateX(layoutWidth) }
        fun getRenderTranslateX(layoutWidth: Float) = if (displayFromRight && layoutWidth > rowWidth) layoutWidth - rowWidth else 0f
    }
    
    private class Block(val startLine: Int, val originalLineCount: Int, val defaultRowHeight: Int) {
        var isMeasured = false
        var rowCount = originalLineCount
        var height = originalLineCount * defaultRowHeight
        var startRow = 0
        var yOffset = 0
        var regions: List<RowRegion>? = null
    }

    inner class WordwrapLayoutRowItr(private val initRow: Int) : RowIterator {
        private val result = Row(); private var currentRow = initRow
        override fun next(): Row {
             if (!hasNext()) throw NoSuchElementException()
             val r = getRowAt(currentRow)
             currentRow++
             return r
        }
        override fun hasNext() = currentRow < rowCount
        override fun reset() { currentRow = initRow }
    }
    
    companion object { private val S_SPANS_FOR_WORDWRAP = listOf(SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 0, true, true, false))) }
}
