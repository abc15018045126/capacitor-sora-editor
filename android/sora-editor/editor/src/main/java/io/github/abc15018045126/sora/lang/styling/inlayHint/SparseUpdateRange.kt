

package io.github.abc15018045126.sora.lang.styling.inlayHint

import android.util.SparseBooleanArray
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import java.lang.Integer.min

class SparseUpdateRange : StyleUpdateRange {

    val array = SparseBooleanArray()

    fun addLine(line: Int) {
        array.put(line, true)
    }

    override fun isInRange(line: Int) = array[line]


    override fun lineIndexIterator(maxLineIndex: Int) = object : IntIterator() {

        var index = 0

        override fun hasNext() = index < array.size()

        override fun nextInt() = min(maxLineIndex, array.keyAt(index++))

    }

}
