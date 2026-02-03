package io.github.abc15018045126.sora.text

import androidx.annotation.VisibleForTesting
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import java.util.Collections
import kotlin.math.abs

class CachedIndexer internal constructor(private val content: Content) : Indexer, ContentListener {
    private val startPosition = CharPosition().toBOF()
    private val endPosition = CharPosition()
    private val cachedPositions = ArrayList<CharPosition>()
    private val thresholdLine = 50
    private var thresholdIndex = 50
    var maxCacheCount = 50

    init { updateEnd() }

    fun setThresholdIndex(s: Int) { thresholdIndex = s }

    private fun updateEnd() {
        endPosition.index = content.length
        endPosition.line = content.lineCount - 1
        endPosition.column = content.getColumnCount(endPosition.line)
    }

    @Synchronized private fun findNearestByIndex(index: Int): CharPosition {
        var minDistance = index
        var nearest = startPosition
        var targetIndex = 0
        for (i in cachedPositions.indices.reversed()) {
            val pos = cachedPositions[i]
            val dis = abs(pos.index - index)
            if (dis < minDistance) {
                minDistance = dis
                nearest = pos
                targetIndex = i
            }
            if (dis <= thresholdIndex) break
        }
        if (abs(endPosition.index - index) < minDistance) nearest = endPosition
        if (nearest !== startPosition && nearest !== endPosition) Collections.swap(cachedPositions, targetIndex, cachedPositions.size - 1)
        return nearest
    }

    @Synchronized private fun findNearestByLine(line: Int): CharPosition {
        var minDistance = line
        var nearest = startPosition
        var targetIndex = 0
        for (i in cachedPositions.indices.reversed()) {
            val pos = cachedPositions[i]
            val dis = abs(pos.line - line)
            if (dis < minDistance) {
                minDistance = dis
                nearest = pos
                targetIndex = i
            }
            if (minDistance <= thresholdLine) break
        }
        if (abs(endPosition.line - line) < minDistance) nearest = endPosition
        if (nearest !== startPosition && nearest !== endPosition) Collections.swap(cachedPositions, targetIndex, cachedPositions.size - 1)
        return nearest
    }

    @VisibleForTesting fun findIndexForward(start: CharPosition, index: Int, dest: CharPosition) {
        if (start.index > index) throw IllegalArgumentException("Unable to find backward from method findIndexForward()")
        var workLine = start.line
        var workColumn = start.column
        var workIndex = start.index
        run {
            val line = content.lines[workLine]
            val sepLen = line.lineSeparatorSafe.length
            workIndex += line.length + (if (sepLen > 0) sepLen - 1 else 0) - workColumn
            workColumn = line.length + (if (sepLen > 0) sepLen - 1 else 0)
        }
        while (workIndex < index) {
            workLine++
            val line = content.lines[workLine]
            val sepLen = line.lineSeparatorSafe.length
            val addition = if (sepLen > 0) sepLen - 1 else 0
            workColumn = line.length + addition
            workIndex += workColumn + 1
        }
        if (workIndex > index) workColumn -= workIndex - index
        dest.column = workColumn
        dest.line = workLine
        dest.index = index
    }

    @VisibleForTesting fun findIndexBackward(start: CharPosition, index: Int, dest: CharPosition) {
        if (start.index < index) throw IllegalArgumentException("Unable to find forward from method findIndexBackward()")
        var workLine = start.line
        var workColumn = start.column
        var workIndex = start.index
        while (workIndex > index) {
            workIndex -= workColumn + 1
            workLine--
            if (workLine != -1) {
                val line = content.lines[workLine]
                val sepLen = line.lineSeparatorSafe.length
                workColumn = line.length + if (sepLen > 0) sepLen - 1 else 0
            } else {
                findIndexForward(startPosition, index, dest)
                return
            }
        }
        if (index > workIndex) { workLine++; workColumn = index - workIndex - 1 }
        dest.column = workColumn
        dest.line = workLine
        dest.index = index
    }

    @VisibleForTesting fun findLiCoForward(start: CharPosition, line: Int, column: Int, dest: CharPosition) {
        if (start.line > line) throw IllegalArgumentException("can not find backward from findLiCoForward()")
        var workLine = start.line
        var workIndex = start.index - start.column
        while (workLine < line) {
            val lineObj = content.lines[workLine]
            workIndex += lineObj.length + lineObj.lineSeparatorSafe.length
            workLine++
        }
        dest.column = 0
        dest.line = workLine
        dest.index = workIndex
        findInLine(dest, line, column)
    }

    @VisibleForTesting fun findLiCoBackward(start: CharPosition, line: Int, column: Int, dest: CharPosition) {
        if (start.line < line) throw IllegalArgumentException("can not find forward from findLiCoBackward()")
        var workLine = start.line
        var workIndex = start.index - start.column
        while (workLine > line) {
            val lineObj = content.lines[workLine - 1]
            workIndex -= lineObj.length + lineObj.lineSeparatorSafe.length
            workLine--
        }
        dest.column = 0
        dest.line = workLine
        dest.index = workIndex
        findInLine(dest, line, column)
    }

    private fun findInLine(pos: CharPosition, line: Int, column: Int) {
        if (pos.line != line) throw IllegalArgumentException("can not find other lines with findInLine()")
        pos.index = pos.index - pos.column + column
        pos.column = column
    }

    @Synchronized private fun push(pos: CharPosition) {
        if (maxCacheCount <= 0) return
        cachedPositions.add(pos)
        if (cachedPositions.size > maxCacheCount) cachedPositions.removeAt(0)
    }

    override fun getCharIndex(line: Int, column: Int) = getCharPosition(line, column).index
    override fun getCharLine(index: Int) = getCharPosition(index).line
    override fun getCharColumn(index: Int) = getCharPosition(index).column
    override fun getCharPosition(index: Int) = CharPosition().also { getCharPosition(index, it) }

    override fun getCharPosition(index: Int, dest: CharPosition) {
        content.checkIndex(index, Content.CHECK_TYPE_INDEX)
        content.lock(false)
        try {
            val pos = findNearestByIndex(index)
            when {
                pos.index == index -> dest.set(pos)
                pos.index < index -> findIndexForward(pos, index, dest)
                else -> findIndexBackward(pos, index, dest)
            }
            if (abs(index - pos.index) >= thresholdIndex) push(dest.fromThis())
        } finally {
            content.unlock(false)
        }
    }

    override fun getCharPosition(line: Int, column: Int) = CharPosition().also { getCharPosition(line, column, it) }

    override fun getCharPosition(line: Int, column: Int, dest: CharPosition) {
        content.checkLineAndColumn(line, column, Content.CHECK_TYPE_INDEX)
        content.lock(false)
        try {
            val pos = findNearestByLine(line)
            if (pos.line == line) {
                dest.set(pos)
                if (pos.column != column) findInLine(dest, line, column)
            } else if (pos.line < line) findLiCoForward(pos, line, column, dest)
            else findLiCoBackward(pos, line, column, dest)
            if (abs(pos.line - line) > thresholdLine) push(dest.fromThis())
        } finally {
            content.unlock(false)
        }
    }

    @UnsupportedUserUsage override fun beforeReplace(content: Content) {}

    @Synchronized @UnsupportedUserUsage override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
        for (pos in cachedPositions) {
            if (pos.line == startLine && pos.column >= startColumn) {
                pos.index += insertedContent.length
                pos.line += endLine - startLine
                pos.column = endColumn + pos.column - startColumn
            } else if (pos.line > startLine) {
                pos.index += insertedContent.length
                pos.line += endLine - startLine
            }
        }
        updateEnd()
    }

    @Synchronized @UnsupportedUserUsage override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
        val iterator = cachedPositions.iterator()
        while (iterator.hasNext()) {
            val pos = iterator.next()
            if (pos.line == startLine) {
                if (pos.column >= startColumn) iterator.remove()
            } else if (pos.line > startLine) {
                if (pos.line <= endLine) iterator.remove()
                else {
                    pos.index -= deletedContent.length
                    pos.line -= endLine - startLine
                }
            }
        }
        updateEnd()
    }
}
