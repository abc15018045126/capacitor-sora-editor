package io.github.abc15018045126.sora.text.breaker

import io.github.abc15018045126.sora.text.ContentLine

class WordBreakerProgram(text: ContentLine) : WordBreakerIcu(text) {

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        val icuResult = super.getOptimizedBreakPoint(start, end)
        if (icuResult != end || end <= start ||   Character.isWhitespace(chars[end - 1])) {
            return icuResult
        }

        var index = end - 1
        while (index > start) {
            if (chars[index] == '.' && index - 1 >= start && !Character.isDigit(chars[index - 1])) {

                return index + 1
            }
            index--
        }
        return end
    }
}
