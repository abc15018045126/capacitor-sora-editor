package io.github.abc15018045126.sora.text

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import androidx.annotation.RequiresApi

object TextUtilsP {
    private const val LF = 0x0A; private const val CR = 0x0D

    @RequiresApi(Build.VERSION_CODES.N)
    private fun isVS(cp: Int) = UCharacter.hasBinaryProperty(cp, UProperty.VARIATION_SELECTOR)

    @JvmStatic @RequiresApi(Build.VERSION_CODES.P)
    fun getOffsetForBackspaceKey(text: CharSequence, offset: Int): Int {
        if (offset <= 1) return 0
        val S_START=0; val S_LF=1; val S_BK=2; val S_BVSK=3; val S_BEM=4; val S_BVSEM=5; val S_BVS=6; val S_BE=7; val S_BZWJ=8; val S_BVSZWJ=9; val S_ORIS=10; val S_ERIS=11; val S_ITS=12; val S_FIN=13
        var del = 0; var vsCount = 0; var state = S_START; var tmp = offset
        do {
            val cp = Character.codePointBefore(text, tmp); tmp -= Character.charCount(cp)
            when (state) {
                S_START -> {
                    del = Character.charCount(cp)
                    state = when {
                        cp == LF -> S_LF
                        isVS(cp) -> S_BVS
                        AndroidEmoji.isRegionalIndicatorSymbol(cp) -> S_ORIS
                        AndroidEmoji.isEmojiModifier(cp) -> S_BEM
                        cp == AndroidEmoji.COMBINING_ENCLOSING_KEYCAP -> S_BK
                        AndroidEmoji.isEmoji(cp) -> S_BE
                        cp == AndroidEmoji.CANCEL_TAG -> S_ITS
                        else -> S_FIN
                    }
                }
                S_LF -> { if (cp == CR) del++; state = S_FIN }
                S_ORIS -> if (AndroidEmoji.isRegionalIndicatorSymbol(cp)) { del += 2; state = S_ERIS } else state = S_FIN
                S_ERIS -> if (AndroidEmoji.isRegionalIndicatorSymbol(cp)) { del -= 2; state = S_ORIS } else state = S_FIN
                S_BK -> if (isVS(cp)) { vsCount = Character.charCount(cp); state = S_BVSK } else { if (AndroidEmoji.isKeycapBase(cp)) del += Character.charCount(cp); state = S_FIN }
                S_BVSK -> { if (AndroidEmoji.isKeycapBase(cp)) del += vsCount + Character.charCount(cp); state = S_FIN }
                S_BEM -> if (isVS(cp)) { vsCount = Character.charCount(cp); state = S_BVSEM } else if (AndroidEmoji.isEmojiModifierBase(cp)) { del += Character.charCount(cp); state = S_BE } else state = S_FIN
                S_BVSEM -> { if (AndroidEmoji.isEmojiModifierBase(cp)) del += vsCount + Character.charCount(cp); state = S_FIN }
                S_BVS -> if (AndroidEmoji.isEmoji(cp)) { del += Character.charCount(cp); state = S_BE } else { if (!isVS(cp) && UCharacter.getCombiningClass(cp) == 0) del += Character.charCount(cp); state = S_FIN }
                S_BE -> state = if (cp == AndroidEmoji.ZERO_WIDTH_JOINER) S_BZWJ else S_FIN
                S_BZWJ -> if (AndroidEmoji.isEmoji(cp)) { del += Character.charCount(cp) + 1; state = if (AndroidEmoji.isEmojiModifier(cp)) S_BEM else S_BE } else if (isVS(cp)) { vsCount = Character.charCount(cp); state = S_BVSZWJ } else state = S_FIN
                S_BVSZWJ -> { if (AndroidEmoji.isEmoji(cp)) { del += vsCount + 1 + Character.charCount(cp); vsCount = 0; state = S_BE } else state = S_FIN }
                S_ITS -> when {
                    AndroidEmoji.isTagSpecChar(cp) -> del += 2
                    AndroidEmoji.isEmoji(cp) -> { del += Character.charCount(cp); state = S_FIN }
                    else -> { del = 2; state = S_FIN }
                }
                else -> throw IllegalArgumentException("state $state unknown")
            }
        } while (tmp > 0 && state != S_FIN)
        return offset - del
    }
}
