package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair


class CharPosition @JvmOverloads constructor(

    @JvmField var line: Int = 0,

    @JvmField var column: Int = 0,

    @JvmField var index: Int = -1
) {

    fun getLine(): Int = line
    fun getColumn(): Int = column
    fun getIndex(): Int = index


    fun toBOF(): CharPosition {
        index = 0
        line = 0
        column = 0
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other is CharPosition) {
            return other.column == column &&
                    other.line == line &&
                    other.index == index
        }
        return false
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + line
        result = 31 * result + column
        return result
    }


    fun toIntPair(): Long {
        return IntPair.pack(line, column)
    }


    fun fromThis(): CharPosition {
        val pos = CharPosition()
        pos.set(this)
        return pos
    }


    fun set(another: CharPosition) {
        index = another.index
        line = another.line
        column = another.column
    }

    override fun toString(): String {
        return "CharPosition(line = $line,column = $column,index = $index)"
    }
}
