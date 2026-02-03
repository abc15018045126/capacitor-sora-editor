package io.github.abc15018045126.sora.text

import java.text.CharacterIterator

class CharSequenceIterator(private val src: CharSequence) : CharacterIterator {
    private var index = 0

    override fun first() = setIndex(0)
    override fun last() = setIndex((src.length - 1).coerceAtLeast(0))
    override fun current() = if (index == src.length) CharacterIterator.DONE else src[index]
    override fun next() = setIndex(index + 1)
    override fun previous() = setIndex((index - 1).coerceAtLeast(0))
    override fun setIndex(i: Int) = i.also { index = it }.let { current() }
    override fun getBeginIndex() = 0
    override fun getEndIndex() = src.length
    override fun getIndex() = index
    override fun clone() = CharSequenceIterator(src).also { it.index = index }
}
