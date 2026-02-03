package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair
import kotlin.math.*

class Cursor(private val content: Content) {
    private val indexer = CachedIndexer(content)
    private var leftSel = CharPosition().toBOF()
    private var rightSel = CharPosition().toBOF()
    private var cache0: CharPosition? = null
    private var cache1: CharPosition? = null
    private var cache2: CharPosition? = null
    private var selDirection = DIRECTION_NONE

    fun set(l: Int, c: Int) {
        leftSel = indexer.getCharPosition(l, c).fromThis()
        rightSel = leftSel.fromThis()
    }

    fun setLeft(l: Int, c: Int) { leftSel = indexer.getCharPosition(l, c).fromThis() }
    fun setRight(l: Int, c: Int) { rightSel = indexer.getCharPosition(l, c).fromThis() }

    val leftLine get() = leftSel.line
    val leftColumn get() = leftSel.column
    val rightLine get() = rightSel.line
    val rightColumn get() = rightSel.column

    fun isInSelectedRegion(l: Int, c: Int): Boolean {
        if (l < leftLine || l > rightLine) return false
        return when (l) {
            leftLine -> if (l == rightLine) c in leftColumn until rightColumn else c >= leftColumn
            rightLine -> c < rightColumn
            else -> true
        }
    }

    val left get() = leftSel.index
    val right get() = rightSel.index

    fun updateCache(line: Int) { indexer.getCharIndex(line, 0) }
    fun getIndexer() = indexer
    fun isSelected() = leftSel.index != rightSel.index
    fun setSelectionDirection(dir: Int) { selDirection = dir }
    fun getSelectionDirection() = selDirection

    fun getLeftOf(pos: Long): Long {
        val l = IntPair.getFirst(pos)
        val c = IntPair.getSecond(pos)
        val nc = TextLayoutHelper.get().getCurPosLeft(c, content.getLine(l))
        return if (nc == c && c == 0) {
            if (l == 0) 0L else IntPair.pack(l - 1, content.getColumnCount(l - 1))
        } else IntPair.pack(l, nc)
    }

    fun getRightOf(pos: Long): Long {
        val l = IntPair.getFirst(pos)
        val c = IntPair.getSecond(pos)
        val maxC = content.getColumnCount(l)
        val nc = TextLayoutHelper.get().getCurPosRight(c, content.getLine(l))
        return if (nc == maxC && c == nc) {
            if (l + 1 == content.lineCount) IntPair.pack(l, maxC) else IntPair.pack(l + 1, 0)
        } else IntPair.pack(l, nc)
    }

    fun left() = leftSel.fromThis()
    fun right() = rightSel.fromThis()
    fun getRange() = TextRange(left(), right())

    internal fun beforeInsert(sl: Int, sc: Int) { cache0 = indexer.getCharPosition(sl, sc).fromThis() }
    internal fun beforeDelete(sl: Int, sc: Int, el: Int, ec: Int) {
        cache1 = indexer.getCharPosition(sl, sc).fromThis()
        cache2 = indexer.getCharPosition(el, ec).fromThis()
    }
    internal fun beforeReplace() = indexer.beforeReplace(content)

    internal fun afterInsert(sl: Int, sc: Int, el: Int, ec: Int, text: CharSequence) {
        indexer.afterInsert(content, sl, sc, el, ec, text)
        val b = cache0?.index ?: 0
        if (left >= b) leftSel = indexer.getCharPosition(left + text.length).fromThis()
        if (right >= b) rightSel = indexer.getCharPosition(right + text.length).fromThis()
    }

    internal fun afterDelete(sl: Int, sc: Int, el: Int, ec: Int, text: CharSequence) {
        indexer.afterDelete(content, sl, sc, el, ec, text)
        val b = cache1?.index ?: 0
        val e = cache2?.index ?: 0
        if (b > right) return
        val nl = left - max(0, min(left - b, e - b))
        val nr = right - max(0, min(right - b, e - b))
        leftSel = indexer.getCharPosition(nl).fromThis()
        rightSel = indexer.getCharPosition(nr).fromThis()
    }

    companion object {
        const val DIRECTION_NONE = 0
        const val DIRECTION_LTR = 1
        const val DIRECTION_RTL = 2
    }
}
