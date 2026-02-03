package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import androidx.annotation.Size
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.ContentListener


interface Layout : ContentListener {


    fun destroyLayout()


    fun getLineNumberForRow(row: Int): Int


    fun obtainRowIterator(initialRow: Int): RowIterator {
        return obtainRowIterator(initialRow, null)
    }


    fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator


    fun getRowAt(rowIndex: Int): Row


    val layoutWidth: Int


    val layoutHeight: Int


    val rowCount: Int


    fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long


    @Size(2)
    fun getCharLayoutOffset(line: Int, column: Int): FloatArray {
        return getCharLayoutOffset(line, column, FloatArray(2))
    }


    fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray


    fun getRowCountForLine(line: Int): Int


    fun getUpPosition(line: Int, column: Int): Long


    fun getDownPosition(line: Int, column: Int): Long


    fun getRowIndexForPosition(index: Int): Int


    fun invalidateLines(range: StyleUpdateRange)


    fun getRowTop(row: Int): Int


    fun getRowBottom(row: Int): Int


    fun getRowIndexForY(y: Float): Int
}
