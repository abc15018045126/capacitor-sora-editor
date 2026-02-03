package io.github.abc15018045126.sora.text

interface Indexer {
    fun getCharIndex(line: Int, column: Int): Int
    fun getCharLine(index: Int): Int
    fun getCharColumn(index: Int): Int
    fun getCharPosition(index: Int): CharPosition
    fun getCharPosition(line: Int, column: Int): CharPosition
    fun getCharPosition(index: Int, dest: CharPosition)
    fun getCharPosition(line: Int, column: Int, dest: CharPosition)
}
