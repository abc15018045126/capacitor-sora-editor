package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextUtils
import io.github.abc15018045126.sora.util.Chars
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.Numbers
import kotlin.math.ceil

private typealias SelectionMovementComputeFunc = ((CodeEditor, CharPosition) -> CharPosition)

enum class SelectionMovement(
    private val computeFunc: SelectionMovementComputeFunc,
    val basePosition: MovingBasePosition = MovingBasePosition.SELECTION_ANCHOR
) {
    UP({ e, p ->
        val res = e.layout!!.getUpPosition(p.line, p.column)
        e.text.indexer.getCharPosition(IntPair.getFirst(res), IntPair.getSecond(res))
    }, MovingBasePosition.LEFT_SELECTION),

    DOWN({ e, p ->
        val res = e.layout!!.getDownPosition(p.line, p.column)
        e.text.indexer.getCharPosition(IntPair.getFirst(res), IntPair.getSecond(res))
    }, MovingBasePosition.RIGHT_SELECTION),

    LEFT({ e, p ->
        val res = e.cursor!!.getLeftOf(p.toIntPair())
        e.text.indexer.getCharPosition(IntPair.getFirst(res), IntPair.getSecond(res))
    }, MovingBasePosition.LEFT_SELECTION),

    RIGHT({ e, p ->
        val res = e.cursor!!.getRightOf(p.toIntPair())
        e.text.indexer.getCharPosition(IntPair.getFirst(res), IntPair.getSecond(res))
    }, MovingBasePosition.RIGHT_SELECTION),

    PREVIOUS_WORD_BOUNDARY({ e, p -> Chars.prevWordStart(p, e.text).let { e.text.indexer.getCharPosition(it.line, it.column) } }),
    NEXT_WORD_BOUNDARY({ e, p -> Chars.nextWordEnd(p, e.text).let { e.text.indexer.getCharPosition(it.line, it.column) } }),

    PAGE_UP({ e, p ->
        val l = e.layout!!
        val count = ceil(e.height / e.rowHeight.toFloat()).toInt()
        val cur = l.getRowIndexForPosition(p.index)
        val after = Numbers.coerceIn(cur - count, 0, l.rowCount - 1)
        val off = p.column - l.getRowAt(cur).startColumn
        l.getRowAt(after).let { r -> e.text.indexer.getCharPosition(r.lineIndex, r.startColumn + Numbers.coerceIn(off, 0, r.endColumn - r.startColumn)) }
    }),

    PAGE_DOWN({ e, p ->
        val l = e.layout!!
        val count = ceil(e.height / e.rowHeight.toFloat()).toInt()
        val cur = l.getRowIndexForPosition(p.index)
        val after = Numbers.coerceIn(cur + count, 0, l.rowCount - 1)
        val off = p.column - l.getRowAt(cur).startColumn
        l.getRowAt(after).let { r -> e.text.indexer.getCharPosition(r.lineIndex, r.startColumn + Numbers.coerceIn(off, 0, r.endColumn - r.startColumn)) }
    }),

    PAGE_TOP({ e, p ->
        val l = e.layout!!
        val cur = l.getRowIndexForPosition(p.index)
        val off = p.column - l.getRowAt(cur).startColumn
        l.getRowAt(e.firstVisibleRow).let { r -> e.text.indexer.getCharPosition(r.lineIndex, r.startColumn + Numbers.coerceIn(off, 0, r.endColumn - r.startColumn)) }
    }),

    PAGE_BOTTOM({ e, p ->
        val l = e.layout!!
        val cur = l.getRowIndexForPosition(p.index)
        val off = p.column - l.getRowAt(cur).startColumn
        l.getRowAt(e.lastVisibleRow).let { r -> e.text.indexer.getCharPosition(r.lineIndex, r.startColumn + Numbers.coerceIn(off, 0, r.endColumn - r.startColumn)) }
    }),

    LINE_START({ e, p ->
        val col = if (e.props!!.enhancedHomeAndEnd) {
            val res = TextUtils.findLeadingAndTrailingWhitespacePos(e.text.getLine(p.line))
            val start = IntPair.getFirst(res)
            if (p.column == start || start == e.text.getColumnCount(p.line)) 0 else start
        } else 0
        e.text.indexer.getCharPosition(p.line, col)
    }),

    LINE_END({ e, p ->
        val total = e.text.getColumnCount(p.line)
        val col = if (e.props!!.enhancedHomeAndEnd) {
            val end = IntPair.getSecond(TextUtils.findLeadingAndTrailingWhitespacePos(e.text.getLine(p.line)))
            if (p.column != end) end else total
        } else total
        e.text.indexer.getCharPosition(p.line, col)
    }),

    TEXT_START({ _, _ -> CharPosition().toBOF() }),
    TEXT_END({ e, _ -> e.text.indexer.getCharPosition(e.text.length) }),

    ROW_START({ e, p ->
        val l = e.layout!!
        val idx = l.getRowIndexForPosition(p.index)
        val r = l.getRowAt(idx)
        val maxCol = if (idx + 1 == l.rowCount || l.getRowAt(idx + 1).lineIndex != r.lineIndex) r.endColumn else r.endColumn - 1
        val col = if (e.props!!.enhancedHomeAndEnd) {
            val start = IntPair.getFirst(TextUtils.findLeadingAndTrailingWhitespacePos(e.text.getLine(p.line), r.startColumn, maxCol))
            if (p.column == start || start == maxCol) r.startColumn else start
        } else r.startColumn
        e.text.indexer.getCharPosition(p.line, col)
    }),

    ROW_END({ e, p ->
        val l = e.layout!!
        val idx = l.getRowIndexForPosition(p.index)
        val r = l.getRowAt(idx)
        val maxCol = if (idx + 1 == l.rowCount || l.getRowAt(idx + 1).lineIndex != r.lineIndex) r.endColumn else r.endColumn - 1
        val col = if (e.props!!.enhancedHomeAndEnd) {
            val end = IntPair.getSecond(TextUtils.findLeadingAndTrailingWhitespacePos(e.text.getLine(p.line), r.startColumn, maxCol))
            if (p.column != end) end else maxCol
        } else maxCol
        e.text.indexer.getCharPosition(p.line, col)
    });

    enum class MovingBasePosition { LEFT_SELECTION, RIGHT_SELECTION, SELECTION_ANCHOR }

    @UnsupportedUserUsage
    fun getPositionAfterMovement(e: CodeEditor, p: CharPosition) = computeFunc(e, p)
}
