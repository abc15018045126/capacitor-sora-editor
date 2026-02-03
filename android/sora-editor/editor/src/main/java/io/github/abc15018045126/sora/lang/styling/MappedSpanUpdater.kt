package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


object MappedSpanUpdater {

    @JvmStatic
    fun shiftSpansOnMultiLineDelete(
        map: MutableList<MutableList<Span>>,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ) {
        var lineCount = endLine - startLine - 1

        while (lineCount > 0) {
            SpanFactory.recycleAll(map.removeAt(startLine + 1))
            lineCount--
        }

        val startLineSpans = map[startLine]
        var index = startLineSpans.size - 1
        while (index > 0) {
            if (startLineSpans[index].column >= startColumn) {
                startLineSpans.removeAt(index).recycle()
                index--
            } else {
                break
            }
        }

        val endLineSpans = map.removeAt(startLine + 1)
        for (i in endLineSpans.indices) {
            endLineSpans[i].shiftColumnBy(startColumn - endColumn)
        }
        while (endLineSpans.size > 1) {
            if (endLineSpans[0].column <= startColumn && endLineSpans[1].column <= startColumn) {
                endLineSpans.removeAt(0).recycle()
            } else {
                break
            }
        }
        if (endLineSpans[0].column <= startColumn) {
            endLineSpans[0].column = startColumn
        }
        startLineSpans.addAll(endLineSpans)
    }

    @JvmStatic
    fun shiftSpansOnSingleLineDelete(map: MutableList<MutableList<Span>>?, line: Int, startCol: Int, endCol: Int) {
        if (map.isNullOrEmpty()) {
            return
        }
        val spanList = map[line]
        var startIndex = findSpanIndexFor(spanList, 0, startCol)
        if (startIndex == -1) {

            return
        }
        var endIndex = findSpanIndexFor(spanList, startIndex, endCol)
        if (endIndex == -1) {
            endIndex = spanList.size
        }

        val removeCount = endIndex - startIndex
        for (i in 0 until removeCount) {
            spanList.removeAt(startIndex).recycle()
        }

        val delta = endCol - startCol
        while (startIndex < spanList.size) {
            spanList[startIndex].shiftColumnBy(-delta)
            startIndex++
        }

        if (spanList.isEmpty() || spanList[0].column != 0) {
            spanList.add(0, SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
        }

        var i = 0
        while (i + 1 < spanList.size) {
            if (spanList[i].column >= spanList[i + 1].column) {
                spanList.removeAt(i).recycle()
                i--
            }
            i++
        }
    }

    @JvmStatic
    fun shiftSpansOnSingleLineInsert(map: MutableList<MutableList<Span>>?, line: Int, startCol: Int, endCol: Int) {
        if (map.isNullOrEmpty()) {
            return
        }
        val spanList = map[line]
        var index = findSpanIndexFor(spanList, 0, startCol)
        if (index == -1) {
            return
        }
        val originIndex = index

        val delta = endCol - startCol
        while (index < spanList.size) {
            spanList[index++].shiftColumnBy(delta)
        }

        if (originIndex == 0) {
            val first = spanList[0]
            if (first.column == EditorColorScheme.TEXT_NORMAL && first.hasSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR)) {
                first.column = 0
            } else {
                spanList.add(0, SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
            }
        }
    }

    @JvmStatic
    fun shiftSpansOnMultiLineInsert(
        map: MutableList<MutableList<Span>>,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ) {

        val startLineSpans = map[startLine]
        var extendedSpanIndex = findSpanIndexFor(startLineSpans, 0, startColumn)
        if (extendedSpanIndex == -1) {
            extendedSpanIndex = startLineSpans.size - 1
        }
        if (startLineSpans[extendedSpanIndex].column > startColumn) {
            extendedSpanIndex--
        }
        val extendedSpan: Span
        if (extendedSpanIndex < 0 || extendedSpanIndex >= startLineSpans.size) {
            extendedSpan = SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
        } else {
            extendedSpan = startLineSpans[extendedSpanIndex]
        }

        for (i in 0 until endLine - startLine) {
            val list = mutableListOf<Span>()
            val newSpan = extendedSpan.copy()
            newSpan.column = 0
            list.add(newSpan)
            map.add(startLine + 1, list)
        }

        val endLineSpans = map[endLine]
        var idx = extendedSpanIndex
        while (idx < startLineSpans.size) {
            val span = startLineSpans[idx++]
            val newSpan = span.copy()
            newSpan.column = Math.max(0, span.column - startColumn + endColumn)
            endLineSpans.add(newSpan)
        }
        while (extendedSpanIndex + 1 < startLineSpans.size) {
            startLineSpans.removeAt(startLineSpans.size - 1).recycle()
        }
        if (endLineSpans.size > 1 && endLineSpans[0].column == 0 && endLineSpans[1].column == 0) {
            endLineSpans.removeAt(0).recycle()
        }
    }

    private fun findSpanIndexFor(spans: List<Span>, initialPosition: Int, targetCol: Int): Int {
        for (i in initialPosition until spans.size) {
            if (spans[i].column >= targetCol) {
                return i
            }
        }
        return -1
    }
}
