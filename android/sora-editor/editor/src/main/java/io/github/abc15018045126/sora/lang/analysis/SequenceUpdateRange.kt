

package io.github.abc15018045126.sora.lang.analysis

import java.lang.Integer.min

class SequenceUpdateRange(val startLine: Int, val endLine: Int = Int.MAX_VALUE) : StyleUpdateRange {

    override fun isInRange(line: Int) = line in startLine..endLine

    override fun lineIndexIterator(maxLineIndex: Int) = object : IntIterator() {

        var currentLine = startLine

        override fun hasNext() = currentLine <= min(endLine, maxLineIndex)

        override fun nextInt() = currentLine++

    }

}
