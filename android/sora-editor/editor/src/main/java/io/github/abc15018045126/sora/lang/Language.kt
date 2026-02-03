package io.github.abc15018045126.sora.lang

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.completion.CompletionCancelledException
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference
import io.github.abc15018045126.sora.widget.SymbolPairMatch


interface Language {


    val analyzeManager: AnalyzeManager


    val interruptionLevel: Int


    @WorkerThread
    @Throws(CompletionCancelledException::class)
    fun requireAutoComplete(
        content: ContentReference, position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    )


    @UiThread
    fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int


    @UiThread
    fun getIndentAdvance(
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return getIndentAdvance(content, line, column)
    }


    @UiThread
    fun useTab(): Boolean



    @get:UiThread
    val formatter: Formatter


    @get:UiThread
    val symbolPairs: SymbolPairMatch?


    @get:UiThread
    val newlineHandlers: Array<NewlineHandler>?


    @get:UiThread
    val quickQuoteHandler: QuickQuoteHandler?
        get() = null


    @UiThread
    fun destroy()

    companion object {

        const val INTERRUPTION_LEVEL_STRONG = 0


        const val INTERRUPTION_LEVEL_SLIGHT = 1


        const val INTERRUPTION_LEVEL_NONE = 2
    }

}
