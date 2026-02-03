package io.github.abc15018045126.sora.lang.analysis

import io.github.abc15018045126.sora.lang.brackets.BracketsProvider
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.styling.Styles


interface StyleReceiver {


    fun setStyles(sourceManager: AnalyzeManager, styles: Styles?)


    fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?)


    fun updateStyles(sourceManager: AnalyzeManager, styles: Styles, range: StyleUpdateRange) {
        setStyles(sourceManager, styles)
    }


    fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?)


    fun updateBracketProvider(sourceManager: AnalyzeManager, provider: BracketsProvider?)

}
