

package io.github.abc15018045126.sora.lang.styling.inlayHint

import androidx.collection.IntSet
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange

class IntSetUpdateRange(val lineSet: IntSet) : StyleUpdateRange {

    val lines = IntArray(lineSet.size)

    init {
        var index = 0
        lineSet.forEach { element ->
            lines[index++] = element
        }
    }

    override fun isInRange(line: Int): Boolean {
        return lineSet.contains(line)
    }

    override fun lineIndexIterator(maxLineIndex: Int): IntIterator {
        return object : IntIterator() {
            var index = 0

            override fun nextInt(): Int {
                return if (index < lines.size) lines[index++].coerceAtMost(maxLineIndex) else maxLineIndex
            }

            override fun hasNext() = index < lines.size

        }
    }

}
