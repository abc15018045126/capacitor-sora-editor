package io.github.abc15018045126.sora.widget.layout

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint


class Row {

    @JvmField
    var lineIndex: Int = 0


    @JvmField
    var isLeadingRow: Boolean = false


    @JvmField
    var isTrailingRow: Boolean = false


    @JvmField
    var startColumn: Int = 0


    @JvmField
    var endColumn: Int = 0


    @JvmField
    @JvmSuppressWildcards
    var inlayHints: List<InlayHint> = emptyList()


    @JvmField
    var renderTranslateX: Float = 0f
}
