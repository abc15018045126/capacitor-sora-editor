package io.github.abc15018045126.sora.lang.styling

import androidx.annotation.NonNull
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import java.util.*


class MappedSpans private constructor(@NonNull private val spanMap: MutableList<MutableList<Span>>) : Spans {

    override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineInsert(spanMap, startLine, startColumn, endColumn)
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineInsert(spanMap, startLine, startColumn, endLine, endColumn)
        }
    }

    override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineDelete(spanMap, startLine, startColumn, endColumn)
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineDelete(spanMap, startLine, startColumn, endLine, endColumn)
        }
    }

    override fun read(): Spans.Reader {
        return MappedSpansAccessor()
    }

    override fun supportsModify(): Boolean {
        return true
    }

    override fun modify(): Spans.Modifier {
        return MappedSpansAccessor()
    }

    override fun getLineCount(): Int {
        return spanMap.size
    }


    class Builder @JvmOverloads constructor(lineCapacity: Int = 128) {
        private val spans: MutableList<MutableList<Span>> = ArrayList(lineCapacity)
        private var last: Span? = null


        fun addIfNeeded(spanLine: Int, column: Int, style: Long) {
            val currentLast = last
            if (currentLast != null && currentLast.style == style) {
                return
            }
            add(spanLine, SpanFactory.obtainNoExt(column, style))
        }


        fun add(spanLine: Int, span: Span) {
            var mapLine = spans.size - 1
            if (spanLine == mapLine) {
                spans[spanLine].add(span)
            } else if (spanLine > mapLine) {
                var extendedSpan = last
                if (extendedSpan == null) {
                    extendedSpan = SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
                }
                while (mapLine < spanLine) {
                    val lineSpans = mutableListOf<Span>()
                    lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                    spans.add(lineSpans)
                    mapLine++
                }
                val lineSpans = spans[spanLine]
                if (span.column == 0) {
                    lineSpans.clear()
                }
                lineSpans.add(span)
            } else {
                throw IllegalStateException("Invalid position")
            }
            last = span
        }


        fun determine(line: Int) {
            var mapLine = spans.size - 1
            var extendedSpan = last
            if (extendedSpan == null) {
                extendedSpan = SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
            }
            while (mapLine < line) {
                val lineSpans = mutableListOf<Span>()
                lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                spans.add(lineSpans)
                mapLine++
            }
        }


        fun addNormalIfNull() {
            if (spans.isEmpty()) {
                val spanList = mutableListOf<Span>()
                spanList.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
                spans.add(spanList)
            }
        }

        fun build(): MappedSpans {
            return MappedSpans(spans)
        }
    }

    private inner class MappedSpansAccessor : Spans.Reader, Spans.Modifier {
        private var span: MutableList<Span>? = null

        private fun checkLine() {
            if (span == null) {
                throw IllegalStateException("line must be set first")
            }
        }

        override fun moveToLine(line: Int) {
            if (line == -1) {
                span = null
                return
            }
            span = spanMap[line]
        }

        override fun getSpanCount(): Int {
            checkLine()
            return span!!.size
        }

        override fun getSpanAt(index: Int): Span {
            checkLine()
            return span!![index]
        }

        override fun getSpansOnLine(line: Int): List<Span> {
            return Collections.unmodifiableList(spanMap[line])
        }

        override fun setSpansOnLine(line: Int, spans: List<Span>) {
            val lastLine = spanMap[spanMap.size - 1]
            val extend = lastLine[lastLine.size - 1]
            while (spanMap.size <= line) {
                val list = mutableListOf<Span>()
                list.add(copyAndSetColumn(extend, 0))
                spanMap.add(list)
            }
            spanMap[line] = ArrayList(spans)
        }

        override fun addLineAt(line: Int, spans: List<Span>) {
            spanMap.add(line, ArrayList(spans))
        }

        override fun deleteLineAt(line: Int) {
            spanMap.removeAt(line)
        }
    }

    companion object {
        @JvmStatic
        private fun copyAndSetColumn(s: Span, column: Int): Span {
            val span = s.copy()
            span.column = column
            return span
        }
    }
}
