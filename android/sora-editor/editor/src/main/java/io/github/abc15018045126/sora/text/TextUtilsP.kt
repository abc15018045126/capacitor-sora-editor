
package io.github.abc15018045126.sora.text

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import androidx.annotation.RequiresApi


object TextUtilsP {

    private const val LINE_FEED: Int = 0x0A
    private const val CARRIAGE_RETURN: Int = 0x0D


    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun isVariationSelector(codepoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codepoint, UProperty.VARIATION_SELECTOR)
    }


    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun getOffsetForBackspaceKey(text: CharSequence, offset: Int): Int {
        if (offset <= 1) {
            return 0
        }


        val STATE_START = 0


        val STATE_LF = 1


        val STATE_BEFORE_KEYCAP = 2

        val STATE_BEFORE_VS_AND_KEYCAP = 3


        val STATE_BEFORE_EMOJI_MODIFIER = 4

        val STATE_BEFORE_VS_AND_EMOJI_MODIFIER = 5


        val STATE_BEFORE_VS = 6


        val STATE_BEFORE_EMOJI = 7

        val STATE_BEFORE_ZWJ = 8


        val STATE_BEFORE_VS_AND_ZWJ = 9


        val STATE_ODD_NUMBERED_RIS = 10

        val STATE_EVEN_NUMBERED_RIS = 11


        val STATE_IN_TAG_SEQUENCE = 12


        val STATE_FINISHED = 13

        var deleteCharCount = 0
        var lastSeenVSCharCount = 0

        var state = STATE_START

        var tmpOffset = offset
        do {
            val codePoint = Character.codePointBefore(text, tmpOffset)
            tmpOffset -= Character.charCount(codePoint)

            when (state) {
                STATE_START -> {
                    deleteCharCount = Character.charCount(codePoint)
                    if (codePoint == LINE_FEED) {
                        state = STATE_LF
                    } else if (isVariationSelector(codePoint)) {
                        state = STATE_BEFORE_VS
                    } else if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        state = STATE_ODD_NUMBERED_RIS
                    } else if (AndroidEmoji.isEmojiModifier(codePoint)) {
                        state = STATE_BEFORE_EMOJI_MODIFIER
                    } else if (codePoint == AndroidEmoji.COMBINING_ENCLOSING_KEYCAP) {
                        state = STATE_BEFORE_KEYCAP
                    } else if (AndroidEmoji.isEmoji(codePoint)) {
                        state = STATE_BEFORE_EMOJI
                    } else if (codePoint == AndroidEmoji.CANCEL_TAG) {
                        state = STATE_IN_TAG_SEQUENCE
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_LF -> {
                    if (codePoint == CARRIAGE_RETURN) {
                        ++deleteCharCount
                    }
                    state = STATE_FINISHED
                }
                STATE_ODD_NUMBERED_RIS -> {
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount += 2
                        state = STATE_EVEN_NUMBERED_RIS
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_EVEN_NUMBERED_RIS -> {
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount -= 2
                        state = STATE_ODD_NUMBERED_RIS
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_KEYCAP -> {
                    if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = STATE_BEFORE_VS_AND_KEYCAP
                    } else {
                        if (AndroidEmoji.isKeycapBase(codePoint)) {
                            deleteCharCount += Character.charCount(codePoint)
                        }
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_VS_AND_KEYCAP -> {
                    if (AndroidEmoji.isKeycapBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint)
                    }
                    state = STATE_FINISHED
                }
                STATE_BEFORE_EMOJI_MODIFIER -> {
                    if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = STATE_BEFORE_VS_AND_EMOJI_MODIFIER
                    } else if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = STATE_BEFORE_EMOJI
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_VS_AND_EMOJI_MODIFIER -> {
                    if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint)
                    }
                    state = STATE_FINISHED
                }
                STATE_BEFORE_VS -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = STATE_BEFORE_EMOJI
                    } else {
                        if (!isVariationSelector(codePoint) &&
                            UCharacter.getCombiningClass(codePoint) == 0
                        ) {
                            deleteCharCount += Character.charCount(codePoint)
                        }
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_EMOJI -> {
                    if (codePoint == AndroidEmoji.ZERO_WIDTH_JOINER) {
                        state = STATE_BEFORE_ZWJ
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_ZWJ -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint) + 1
                        state = if (AndroidEmoji.isEmojiModifier(codePoint))
                            STATE_BEFORE_EMOJI_MODIFIER else STATE_BEFORE_EMOJI
                    } else if (isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = STATE_BEFORE_VS_AND_ZWJ
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_BEFORE_VS_AND_ZWJ -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {

                        deleteCharCount += lastSeenVSCharCount + 1 + Character.charCount(codePoint)
                        lastSeenVSCharCount = 0
                        state = STATE_BEFORE_EMOJI
                    } else {
                        state = STATE_FINISHED
                    }
                }
                STATE_IN_TAG_SEQUENCE -> {
                    if (AndroidEmoji.isTagSpecChar(codePoint)) {
                        deleteCharCount += 2

                    } else if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = STATE_FINISHED
                    } else {

                        deleteCharCount = 2
                        state = STATE_FINISHED
                    }

                }
                else -> throw IllegalArgumentException("state $state is unknown")
            }
        } while (tmpOffset > 0 && state != STATE_FINISHED)

        return offset - deleteCharCount
    }
}
