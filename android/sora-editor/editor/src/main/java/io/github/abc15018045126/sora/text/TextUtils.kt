package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.util.IntPair
import kotlin.math.max
import kotlin.math.min

object TextUtils {
    @JvmStatic fun countLeadingSpacesAndTabs(text: CharSequence): Long {
        var p = 0; var s = 0; var tCount = 0
        while (p < text.length && isWhitespace(text[p])) {
            if (text[p] == '\t') tCount++ else s++
            p++
        }
        return IntPair.pack(s, tCount)
    }

    @JvmStatic fun countLeadingSpaceCount(t: CharSequence, w: Int) = countLeadingSpacesAndTabs(t).let { IntPair.getFirst(it) + w * IntPair.getSecond(it) }

    @JvmStatic fun createIndent(size: Int, w: Int, useTab: Boolean) = buildString {
        val s = max(0, size)
        val t = if (useTab) s / w else 0
        repeat(t) { append('\t') }
        repeat(s - (if (useTab) t * w else 0)) { append(' ') }
    }

    @JvmStatic fun indexOf(t: CharSequence, p: CharSequence, ignore: Boolean, from: Int): Int {
        for (i in from..t.length - p.length) {
            if ((0 until p.length).all { j -> val sChar = t[i + j]; val pChar = p[j]; sChar == pChar || (ignore && sChar.lowercaseChar() == pChar.lowercaseChar()) }) return i
        }
        return -1
    }

    @JvmStatic fun lastIndexOf(t: CharSequence, p: CharSequence, ignore: Boolean, from: Int): Int {
        for (i in min(from, t.length - p.length) downTo 0) {
            if ((0 until p.length).all { j -> val sChar = t[i + j]; val pChar = p[j]; sChar == pChar || (ignore && sChar.lowercaseChar() == pChar.lowercaseChar()) }) return i
        }
        return -1
    }

    @JvmStatic fun startsWith(t: CharSequence, p: CharSequence, ignore: Boolean) = t.length >= p.length && (0 until p.length).all { i -> val sChar = t[i]; val pChar = p[i]; sChar == pChar || (ignore && sChar.lowercaseChar() == pChar.lowercaseChar()) }

    private fun isWhitespace(ch: Char) = ch == '\t' || ch == ' '

    @JvmStatic fun padStart(src: String, pad: Char, len: Int) = src.padStart(len, pad)

    @JvmStatic fun findLeadingAndTrailingWhitespacePos(line: ContentLine) = findLeadingAndTrailingWhitespacePos(line, 0, line.length)

    @JvmStatic fun findLeadingAndTrailingWhitespacePos(line: ContentLine, start: Int, end: Int): Long {
        val buf = line.backingCharArray
        var l = start; var t = end
        while (l < end && isWhitespace(buf[l])) l++
        if (l != end) while (t > 0 && isWhitespace(buf[t - 1])) t--
        return IntPair.pack(l, t)
    }
}
