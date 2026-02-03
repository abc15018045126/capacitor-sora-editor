

package io.github.abc15018045126.sora.lang.styling.patching

import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor

class StylePatch(
    var startLine: Int,
    var startColumn: Int,
    var endLine: Int,
    var endColumn: Int
) : Comparable<StylePatch> {

    init {
        if (startLine < 0 || startColumn < 0 || endLine < 0 || endColumn < 0) {
            throw IllegalArgumentException("negative number")
        }
        if (endLine < startLine || (endLine == startLine && endColumn < startColumn)) {
            throw IllegalArgumentException("end < start")
        }
    }

    var overrideForeground: ResolvableColor? = null
    var overrideBackground: ResolvableColor? = null
    var overrideItalics: Boolean? = null
    var overrideBold: Boolean? = null

    override fun compareTo(other: StylePatch): Int {
        var res = startLine.compareTo(other.startLine)
        if (res != 0) return res
        res = startColumn.compareTo(other.startColumn)
        if (res != 0) return res
        res = endLine.compareTo(other.endLine)
        if (res != 0) return res
        return endColumn.compareTo(other.endColumn)
    }
}

