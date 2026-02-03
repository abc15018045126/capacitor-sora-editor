package io.github.abc15018045126.sora.text

import android.icu.text.BreakIterator
import android.os.Build
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.MyCharacter


object ICUUtils {


    @JvmStatic
    fun getWordRange(text: CharSequence, offset: Int, useIcu: Boolean): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && useIcu) {
            val itr = BreakIterator.getWordInstance()
            itr.setText(CharSequenceIterator(text))
            val end = itr.following(offset)
            val start = itr.previous()
            if (offset in start..end) {
                return IntPair.pack(start, end)
            } else {
                return getWordRangeFallback(text, offset)
            }
        } else {
            return getWordRangeFallback(text, offset)
        }
    }


    @JvmStatic
    fun getWordRangeFallback(text: CharSequence, offset: Int): Long {
        var start = offset
        var end = offset
        while (end < text.length && MyCharacter.isJavaIdentifierPart(text[end])) {
            end++
        }
        if (end > offset) {
            while (start > 0 && MyCharacter.isJavaIdentifierPart(text[start - 1])) {
                start--
            }
        }
        return IntPair.pack(start, end)
    }
}
