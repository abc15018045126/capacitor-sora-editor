package io.github.abc15018045126.sora.text

class TextRange(
    @JvmField var start: CharPosition,
    @JvmField var end: CharPosition
) {
    fun getStart() = start
    fun setStart(s: CharPosition) { start = s }
    fun getEnd() = end
    fun setEnd(e: CharPosition) { end = e }

    val startIndex get() = start.index
    val endIndex get() = end.index

    fun isPositionInside(pos: CharPosition) = pos.index in start.index until end.index
    override fun toString() = "TextRange{start=$start, end=$end}"
}
