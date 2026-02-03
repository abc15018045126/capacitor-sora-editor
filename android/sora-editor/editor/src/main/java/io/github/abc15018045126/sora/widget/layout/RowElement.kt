package io.github.abc15018045126.sora.widget.layout

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint


class RowElement {

    @JvmField
    var type: Int = 0




    @JvmField
    var startColumn: Int = 0


    @JvmField
    var endColumn: Int = 0


    @JvmField
    var isRtlText: Boolean = false




    @JvmField
    var inlayHint: InlayHint? = null


    @JvmField
    var displayColumnPosition: Int = 0
}
