package io.github.abc15018045126.sora.widget.component

import android.view.LayoutInflater
import android.view.View
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticDetail
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticRegion
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


interface DiagnosticTooltipLayout {

    fun attach(window: EditorDiagnosticTooltipWindow)

    fun createView(inflater: LayoutInflater): View

    fun applyColorScheme(colorScheme: EditorColorScheme)

    fun renderDiagnostic(diagnostic: DiagnosticDetail?)


    fun renderDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?) {
        renderDiagnostic(diagnostic)
    }


    fun onTextSizeChanged(oldSizePx: Float, newSizePx: Float) {}

    fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int>

    fun isPointerOverPopup(): Boolean

    fun isMenuShowing(): Boolean

    fun onWindowDismissed()
}
