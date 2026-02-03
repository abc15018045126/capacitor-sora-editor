package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair

class CharPosition @JvmOverloads constructor(
    @JvmField var line: Int = 0,
    @JvmField var column: Int = 0,
    @JvmField var index: Int = -1
) {
    fun getLine() = line
    fun getColumn() = column
    fun getIndex() = index

    fun toBOF() = apply { index = 0; line = 0; column = 0 }

    override fun equals(other: Any?) = other is CharPosition && other.column == column && other.line == line && other.index == index
    override fun hashCode() = 31 * (31 * index + line) + column
    fun toIntPair() = IntPair.pack(line, column)
    fun fromThis() = CharPosition(line, column, index)
    fun set(o: CharPosition) { index = o.index; line = o.line; column = o.column }
    override fun toString() = "CharPosition(line=$line, column=$column, index=$index)"
}
