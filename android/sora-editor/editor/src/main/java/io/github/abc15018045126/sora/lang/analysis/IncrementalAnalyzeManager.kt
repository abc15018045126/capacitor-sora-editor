package io.github.abc15018045126.sora.lang.analysis

import io.github.abc15018045126.sora.lang.styling.Span


interface IncrementalAnalyzeManager<S, T> : AnalyzeManager {


    val initialState: S


    fun getState(line: Int): LineTokenizeResult<S, T>?


    fun stateEquals(state: S, another: S): Boolean


    fun tokenizeLine(line: CharSequence, state: S, lineIndex: Int): LineTokenizeResult<S, T>


    fun generateSpansForLine(tokens: LineTokenizeResult<S, T>): List<Span>?


    fun onAbandonState(state: S)


    fun onAddState(state: S)


    class LineTokenizeResult<S_, T_> {


        @JvmField
        var state: S_


        @JvmField
        var tokens: List<T_>?


        @JvmField
        var spans: List<Span>? = null

        constructor(state: S_, tokens: List<T_>?) {
            this.state = state
            this.tokens = tokens
        }

        constructor(state: S_, tokens: List<T_>?, spans: List<Span>?) {
            this.state = state
            this.tokens = tokens
            this.spans = spans
        }

        fun clearSpans(): LineTokenizeResult<S_, T_> {
            spans = null
            return this
        }

    }

}
