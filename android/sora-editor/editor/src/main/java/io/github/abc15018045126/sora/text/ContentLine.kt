package io.github.abc15018045126.sora.text

import android.text.GetChars
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.text.bidi.BidiRequirementChecker
import io.github.abc15018045126.sora.text.bidi.TextBidi
import io.github.abc15018045126.sora.util.ShareableData
import java.util.concurrent.atomic.AtomicInteger

class ContentLine : CharSequence, GetChars, BidiRequirementChecker, ShareableData<ContentLine> {
    internal var value: CharArray
    override var length = 0; internal set
    internal var rtlAffectingCount = 0
    internal var surrogateCount = 0
    private var _lineSeparator: LineSeparator? = null
    var lineSeparator: LineSeparator?
        get() = _lineSeparator
        set(v) { _lineSeparator = v }

    val lineSeparatorSafe get() = _lineSeparator ?: LineSeparator.NONE
    private var refCount: AtomicInteger? = null

    constructor() : this(32)
    constructor(text: CharSequence?) : this((text?.length ?: 0) + 16) { insert(0, text) }
    constructor(src: ContentLine) : this(src.length + 16) {
        length = src.length
        rtlAffectingCount = src.rtlAffectingCount
        surrogateCount = src.surrogateCount
        _lineSeparator = src.lineSeparator
        System.arraycopy(src.value, 0, value, 0, length)
    }

    constructor(size: Int) {
        value = CharArray(size)
    }

    private constructor(init: Boolean, size: Int = 0) {
        value = if (init) CharArray(32) else CharArray(size)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index > length) throw StringIndexOutOfBoundsException("index=$index, length=$length")
    }

    private fun ensureCapacity(cap: Int) {
        if (value.size < cap) {
            val next = if (value.size * 2 < cap) cap + 2 else value.size * 2
            value = CharArray(next).also { System.arraycopy(value, 0, it, 0, length) }
        }
    }

    fun insert(dstOff: Int, s: CharSequence?) = insert(dstOff, s ?: "null", 0, (s ?: "null").length)

    fun insert(dstOff: Int, s: CharSequence?, start: Int, end: Int): ContentLine {
        val str = s ?: "null"
        if (dstOff < 0 || dstOff > length) throw IndexOutOfBoundsException("dstOff $dstOff")
        if (start < 0 || end < 0 || start > end || end > str.length) throw IndexOutOfBoundsException("start $start, end $end")
        val len = end - start
        ensureCapacity(length + len)
        System.arraycopy(value, dstOff, value, dstOff + len, length - dstOff)
        for (i in start until end) {
            val ch = str[i]
            value[dstOff + i - start] = ch
            if (TextBidi.couldAffectRtl(ch)) rtlAffectingCount++
            if (Character.isSurrogate(ch)) surrogateCount++
        }
        length += len
        return this
    }

    fun insert(off: Int, c: Char): ContentLine {
        ensureCapacity(length + 1)
        if (off < length) System.arraycopy(value, off, value, off + 1, length - off)
        if (TextBidi.couldAffectRtl(c)) rtlAffectingCount++
        if (Character.isSurrogate(c)) surrogateCount++
        value[off] = c
        length++
        return this
    }

    fun delete(start: Int, end: Int): ContentLine {
        val e = if (end > length) length else end
        if (start < 0 || start > e) throw StringIndexOutOfBoundsException("start $start, end $e")
        val len = e - start
        if (len > 0) {
            for (i in start until e) {
                if (TextBidi.couldAffectRtl(value[i])) rtlAffectingCount--
                if (Character.isSurrogate(value[i])) surrogateCount--
            }
            System.arraycopy(value, e, value, start, length - e)
            length -= len
        }
        return this
    }

    override fun mayNeedBidi() = rtlAffectingCount > 0
    fun hasSurrogate() = surrogateCount > 0
    fun append(text: CharSequence) = insert(length, text)

    @UnsupportedUserUsage
    override fun get(index: Int): Char {
        if (index >= length) {
            val sep = lineSeparatorSafe
            return if (sep.length > 0) sep.content[index - length] else '\n'
        }
        return value[index]
    }

    override fun subSequence(start: Int, end: Int): ContentLine {
        checkIndex(start); checkIndex(end)
        if (end < start) throw StringIndexOutOfBoundsException("end < start")
        val len = end - start
        val res = ContentLine(false, len + 16)
        System.arraycopy(value, start, res.value, 0, len)
        res.length = len
        if (rtlAffectingCount > 0) {
            for (i in 0 until len) {
                val c = res.value[i]
                if (TextBidi.couldAffectRtl(c)) res.rtlAffectingCount++
                if (Character.isSurrogate(c)) res.surrogateCount++
            }
        }
        return res
    }

    fun appendTo(sb: StringBuilder) = sb.append(value, 0, length)
    override fun toString() = String(value, 0, length)

    fun toStringWithNewline(): String {
        ensureCapacity(length + 1)
        value[length] = '\n'
        return String(value, 0, length + 1)
    }

    val backingCharArray get() = value

    override fun getChars(srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int) {
        if (srcBegin < 0 || srcEnd > length || srcBegin > srcEnd) throw StringIndexOutOfBoundsException()
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin)
    }

    fun copy() = ContentLine(false, value.size).apply {
        length = this@ContentLine.length
        System.arraycopy(this@ContentLine.value, 0, value, 0, length)
        rtlAffectingCount = this@ContentLine.rtlAffectingCount
        surrogateCount = this@ContentLine.surrogateCount
        _lineSeparator = this@ContentLine._lineSeparator
    }

    override fun retain() {
        if (refCount == null) refCount = AtomicInteger(2)
        else refCount!!.incrementAndGet()
    }

    override fun release() {
        if (refCount != null && refCount!!.decrementAndGet() < 0) throw IllegalStateException("No active owner")
    }

    override fun isMutable() = refCount == null || refCount!!.get() == 1
    override fun toMutable() = if (isMutable()) this else copy()
}
