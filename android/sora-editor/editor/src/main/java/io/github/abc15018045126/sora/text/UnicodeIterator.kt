package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.ObjectPool

class UnicodeIterator private constructor() {
    private var text: CharSequence? = null
    var codePoint = 0; private set
    var startIndex = 0; private set
    var endIndex = 0; private set
    private var limit = 0

    fun recycle() = sPool.recycle(this)

    fun set(t: CharSequence, start: Int, end: Int) {
        if ((start or end or (end - start) or (t.length - end)) < 0) throw IndexOutOfBoundsException()
        text = t; startIndex = start; endIndex = start; limit = end
    }

    operator fun hasNext() = endIndex < limit

    fun nextCodePoint(): Int {
        startIndex = endIndex
        if (startIndex >= limit) {
            codePoint = 0
        } else {
            val ch = text!![endIndex++]
            codePoint = if (Character.isHighSurrogate(ch) && endIndex < limit) {
                Character.toCodePoint(ch, text!![endIndex++])
            } else ch.code
        }
        return codePoint
    }

    companion object {
        private val sPool = object : ObjectPool<UnicodeIterator>() {
            override fun allocateNew() = UnicodeIterator()
            override fun onRecycleObject(obj: UnicodeIterator) { obj.text = null }
        }
        @JvmStatic fun obtain(text: CharSequence, start: Int, end: Int) = sPool.obtain().apply { set(text, start, end) }
    }
}
