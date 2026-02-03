package io.github.abc15018045126.sora.text

import android.icu.text.BreakIterator
import android.os.Build
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.MyCharacter

object ICUUtils {
    @JvmStatic fun getWordRange(text: CharSequence, offset: Int, useIcu: Boolean): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && useIcu) {
            val itr = BreakIterator.getWordInstance().apply { setText(CharSequenceIterator(text)) }
            val e = itr.following(offset); val s = itr.previous()
            if (offset in s..e) return IntPair.pack(s, e)
        }
        return getWordRangeFallback(text, offset)
    }

    @JvmStatic fun getWordRangeFallback(text: CharSequence, offset: Int): Long {
        var s = offset; var e = offset
        while (e < text.length && MyCharacter.isJavaIdentifierPart(text[e])) e++
        if (e > offset) while (s > 0 && MyCharacter.isJavaIdentifierPart(text[s - 1])) s--
        return IntPair.pack(s, e)
    }
}
