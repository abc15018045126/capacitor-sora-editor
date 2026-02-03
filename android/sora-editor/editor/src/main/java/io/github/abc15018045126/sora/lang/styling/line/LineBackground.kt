



package io.github.abc15018045126.sora.lang.styling.line

import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor


data class LineBackground(override var line: Int, var color: ResolvableColor) :
    LineAnchorStyle(line)
