
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

    UP({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val newPos = layout.getUpPosition(pos.line, pos.column)
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.LEFT_SELECTION),


    DOWN({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val newPos = layout.getDownPosition(pos.line, pos.column)
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.RIGHT_SELECTION),


    LEFT({ editor, pos ->
        val newPos = editor.cursor!!.getLeftOf(pos.toIntPair())
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.LEFT_SELECTION),


    RIGHT({ editor, pos ->
        val newPos = editor.cursor!!.getRightOf(pos.toIntPair())
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.RIGHT_SELECTION),


    PREVIOUS_WORD_BOUNDARY({ editor, pos ->
        val newPos = Chars.prevWordStart(pos, editor.text)
        editor.text.indexer.getCharPosition(newPos.line, newPos.column)
    }),


    NEXT_WORD_BOUNDARY({ editor, pos ->
        val newPos = Chars.nextWordEnd(pos, editor.text)
        editor.text.indexer.getCharPosition(newPos.line, newPos.column)
    }),


    PAGE_UP({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val rowCount = ceil(editor.height / editor.rowHeight.toFloat()).toInt()
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val afterIdx = Numbers.coerceIn(currIdx - rowCount, 0, layout.rowCount - 1)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val row = layout.getRowAt(afterIdx)

        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),


    PAGE_DOWN({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val rowCount = ceil(editor.height / editor.rowHeight.toFloat()).toInt()
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val afterIdx = Numbers.coerceIn(currIdx + rowCount, 0, layout.rowCount - 1)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val row = layout.getRowAt(afterIdx)

        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),


    PAGE_TOP({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val afterIdx = editor.firstVisibleRow
        val row = layout.getRowAt(afterIdx)

        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),


    PAGE_BOTTOM({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val afterIdx = editor.lastVisibleRow
        val row = layout.getRowAt(afterIdx)

        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),


    LINE_START({ editor, pos ->
        if (editor.props!!.enhancedHomeAndEnd) {

            val column = IntPair.getFirst(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line)
                )
            )
            if (pos.column == column || column == editor.text.getColumnCount(pos.line)) {

                editor.text.indexer.getCharPosition(pos.line, 0)
            } else {
                editor.text.indexer.getCharPosition(pos.line, column)
            }
        } else {
            editor.text.indexer.getCharPosition(pos.line, 0)
        }
    }),


    LINE_END({ editor, pos ->
        val colNum = editor.text.getColumnCount(pos.line)
        if (editor.props!!.enhancedHomeAndEnd) {

            val column = IntPair.getSecond(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line)
                )
            )
            if (pos.column != column) {
                editor.text.indexer.getCharPosition(pos.line, column)
            } else {
                editor.text.indexer.getCharPosition(pos.line, colNum)
            }
        } else {
            editor.text.indexer.getCharPosition(pos.line, colNum)
        }
    }),


    TEXT_START({ _, _ ->
        CharPosition().toBOF()
    }),


    TEXT_END({ editor, _ ->
        editor.text.indexer.getCharPosition(editor.text.length)
    }),


    ROW_START({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!

        val rowIndex = layout.getRowIndexForPosition(pos.index)
        val row = layout.getRowAt(rowIndex)
        val maxColumn =
            if (rowIndex + 1 == layout.rowCount || layout.getRowAt(rowIndex + 1).lineIndex != row.lineIndex) {
                row.endColumn
            } else {
                row.endColumn - 1
            }
        if (editor.props!!.enhancedHomeAndEnd) {

            val column = IntPair.getFirst(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line), row.startColumn, maxColumn
                )
            )
            if (pos.column == column || column == maxColumn) {

                editor.text.indexer.getCharPosition(pos.line, row.startColumn)
            } else {
                editor.text.indexer.getCharPosition(pos.line, column)
            }
        } else {
            editor.text.indexer.getCharPosition(row.lineIndex, row.startColumn)
        }
    }),


    ROW_END({ editor, pos ->
        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!

        val rowIndex = layout.getRowIndexForPosition(pos.index)
        val row = layout.getRowAt(rowIndex)
        val maxColumn =
            if (rowIndex + 1 == layout.rowCount || layout.getRowAt(rowIndex + 1).lineIndex != row.lineIndex) {
                row.endColumn
            } else {
                row.endColumn - 1
            }
        if (editor.props!!.enhancedHomeAndEnd) {

            val column = IntPair.getSecond(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line), row.startColumn, maxColumn
                )
            )
            if (pos.column != column) {
                editor.text.indexer.getCharPosition(pos.line, column)
            } else {
                editor.text.indexer.getCharPosition(pos.line, maxColumn)
            }
        } else {
            editor.text.indexer.getCharPosition(row.lineIndex, maxColumn)
        }
    });


    enum class MovingBasePosition {
        LEFT_SELECTION,
        RIGHT_SELECTION,
        SELECTION_ANCHOR
    }

    @UnsupportedUserUsage
    fun getPositionAfterMovement(editor: CodeEditor, pos: CharPosition): CharPosition {
        return this.computeFunc(editor, pos)
    }
}

