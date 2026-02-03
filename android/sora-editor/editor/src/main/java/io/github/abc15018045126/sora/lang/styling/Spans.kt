package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


interface Spans {


    fun adjustOnInsert(start: CharPosition, end: CharPosition)


    fun adjustOnDelete(start: CharPosition, end: CharPosition)


    fun read(): Reader


    fun supportsModify(): Boolean


    fun modify(): Modifier


    fun getLineCount(): Int


    interface Reader {


        fun moveToLine(line: Int)


        fun getSpanCount(): Int


        fun getSpanAt(index: Int): Span


        fun getSpansOnLine(line: Int): List<Span>
    }


    interface Modifier {


        fun setSpansOnLine(line: Int, spans: List<out Span>)


        fun addLineAt(line: Int, spans: List<out Span>)


        fun deleteLineAt(line: Int)
    }
}
