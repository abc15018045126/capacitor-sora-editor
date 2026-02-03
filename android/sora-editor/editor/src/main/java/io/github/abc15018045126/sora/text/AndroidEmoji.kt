
package io.github.abc15018045126.sora.text

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import androidx.annotation.RequiresApi


object AndroidEmoji {

    const val COMBINING_ENCLOSING_KEYCAP: Int = 0x20E3

    const val ZERO_WIDTH_JOINER: Int = 0x200D

    const val VARIATION_SELECTOR_16: Int = 0xFE0F

    const val CANCEL_TAG: Int = 0xE007F


    @JvmStatic
    fun isRegionalIndicatorSymbol(codePoint: Int): Boolean {
        return codePoint in 0x1F1E6..0x1F1FF
    }


    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmojiModifier(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER)
    }


    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmojiModifierBase(c: Int): Boolean {



        if (c == 0x1F91D || c == 0x1F93C) {
            return true
        }


        return UCharacter.hasBinaryProperty(c, UProperty.EMOJI_MODIFIER_BASE)
    }


    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmoji(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI)
    }


    @JvmStatic
    fun isKeycapBase(codePoint: Int): Boolean {
        return (codePoint in '0'.code..'9'.code) || codePoint == '#'.code || codePoint == '*'.code
    }


    @JvmStatic
    fun isTagSpecChar(codePoint: Int): Boolean {
        return codePoint in 0xE0020..0xE007E
    }
}
