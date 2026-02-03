

package io.github.abc15018045126.sora.lang.styling.inlayHint

import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor


class TextInlayHint(
    line: Int,
    column: Int,
    val text: String
) : InlayHint(line, column, TYPE_NAME) {

    companion object {
        const val TYPE_NAME = "text"
    }

}

class ColorInlayHint(
    line: Int,
    column: Int,
    val color: ResolvableColor
) : InlayHint(line, column, TYPE_NAME) {

    companion object {
        const val TYPE_NAME = "color"
    }

}
