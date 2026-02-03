package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair


class Cursor(private val content: Content) {

    private val indexer: CachedIndexer = CachedIndexer(content)
    private var leftSel: CharPosition = CharPosition().toBOF()
    private var rightSel: CharPosition = CharPosition().toBOF()
    private var cache0: CharPosition? = null
    private var cache1: CharPosition? = null
    private var cache2: CharPosition? = null

    private var selDirection = DIRECTION_NONE


    fun set(line: Int, column: Int) {
        val pos = indexer.getCharPosition(line, column).fromThis()
        leftSel = pos
        rightSel = pos.fromThis()
    }


    fun setLeft(line: Int, column: Int) {
        leftSel = indexer.getCharPosition(line, column).fromThis()
    }


    fun setRight(line: Int, column: Int) {
        rightSel = indexer.getCharPosition(line, column).fromThis()
    }


    val leftLine: Int
        get() = leftSel.line


    val leftColumn: Int
        get() = leftSel.column


    val rightLine: Int
        get() = rightSel.line


    val rightColumn: Int
        get() = rightSel.column


    fun isInSelectedRegion(line: Int, column: Int): Boolean {

        val startLine = leftSel.line
        val endLine = rightSel.line

        if (line < startLine || line > endLine) {
            return false
        }
        if (line == startLine) {
            if (line == endLine) {

                return column >= leftSel.column && column < rightSel.column
            }

            return column >= leftSel.column
        }
        if (line == endLine) {

            return column < rightSel.column
        }

        return true
    }


    val left: Int
        get() = leftSel.index


    val right: Int
        get() = rightSel.index


    fun updateCache(line: Int) {
        indexer.getCharIndex(line, 0)
    }


    fun getIndexer(): CachedIndexer {
        return indexer
    }


    fun isSelected(): Boolean {
        return leftSel.index != rightSel.index
    }


    fun setSelectionDirection(selDirection: Int) {
        this.selDirection = selDirection
    }


    fun getSelectionDirection(): Int {
        return selDirection
    }


    fun getLeftOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val nColumn = TextLayoutHelper.get().getCurPosLeft(column, content.getLine(line))
        return if (nColumn == column && column == 0) {
            if (line == 0) {
                0L
            } else {
                val cColumn = content.getColumnCount(line - 1)
                IntPair.pack(line - 1, cColumn)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }


    fun getRightOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val cColumn = content.getColumnCount(line)
        val nColumn = TextLayoutHelper.get().getCurPosRight(column, content.getLine(line))
        return if (nColumn == cColumn && column == nColumn) {
            if (line + 1 == content.lineCount) {
                IntPair.pack(line, cColumn)
            } else {
                IntPair.pack(line + 1, 0)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }


    fun left(): CharPosition {
        return leftSel.fromThis()
    }


    fun right(): CharPosition {
        return rightSel.fromThis()
    }


    fun getRange(): TextRange {
        return TextRange(left(), right())
    }


    internal fun beforeInsert(startLine: Int, startColumn: Int) {
        cache0 = indexer.getCharPosition(startLine, startColumn).fromThis()
    }


    internal fun beforeDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        cache1 = indexer.getCharPosition(startLine, startColumn).fromThis()
        cache2 = indexer.getCharPosition(endLine, endColumn).fromThis()
    }


    internal fun beforeReplace() {
        indexer.beforeReplace(content)
    }


    internal fun afterInsert(
        startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        insertedContent: CharSequence
    ) {
        indexer.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        val beginIdx = cache0?.index ?: 0
        if (left >= beginIdx) {
            leftSel = indexer.getCharPosition(left + insertedContent.length).fromThis()
        }
        if (right >= beginIdx) {
            rightSel = indexer.getCharPosition(right + insertedContent.length).fromThis()
        }
    }


    internal fun afterDelete(
        startLine: Int, startColumn: Int, endLine: Int, endColumn: Int,
        deletedContent: CharSequence
    ) {
        indexer.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        val beginIdx = cache1?.index ?: 0
        val endIdx = cache2?.index ?: 0
        if (beginIdx > right) {
            return
        }
        val left = left - Math.max(0, Math.min(left - beginIdx, endIdx - beginIdx))
        val right = right - Math.max(0, Math.min(right - beginIdx, endIdx - beginIdx))
        leftSel = indexer.getCharPosition(left).fromThis()
        rightSel = indexer.getCharPosition(right).fromThis()
    }

    companion object {
        const val DIRECTION_NONE = 0
        const val DIRECTION_LTR = 1
        const val DIRECTION_RTL = 2
    }
}
