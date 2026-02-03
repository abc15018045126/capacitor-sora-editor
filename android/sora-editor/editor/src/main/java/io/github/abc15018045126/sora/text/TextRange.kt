package io.github.abc15018045126.sora.text


class TextRange(
    @JvmField var start: CharPosition,
    @JvmField var end: CharPosition
) {

    fun getStart(): CharPosition {
        return start
    }

    fun setStart(start: CharPosition) {
        this.start = start
    }

    fun getEnd(): CharPosition {
        return end
    }

    fun setEnd(end: CharPosition) {
        this.end = end
    }

    val startIndex: Int
        get() = start.index

    val endIndex: Int
        get() = end.index


    fun isPositionInside(pos: CharPosition): Boolean {
        return pos.index >= start.index && pos.index < end.index
    }

    override fun toString(): String {
        return "TextRange{" +
                "start=" + start +
                ", end=" + end +
                '}'
    }
}
