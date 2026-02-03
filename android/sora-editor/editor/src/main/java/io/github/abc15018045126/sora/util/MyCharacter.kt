package io.github.abc15018045126.sora.util

import java.util.Arrays

object MyCharacter {
    private val bitsIsStart = IntArray(2048)
    private val bitsIsPart = IntArray(2048)

    init {
        for (i in 0..65535) {
            val c = i.toChar()
            if (Character.isJavaIdentifierPart(c)) bitsIsPart[i shr 5] = bitsIsPart[i shr 5] or (1 shl (i and 31))
            if (Character.isJavaIdentifierStart(c)) bitsIsStart[i shr 5] = bitsIsStart[i shr 5] or (1 shl (i and 31))
        }
    }

    @Deprecated("The class will be initialized automatically")
    @JvmStatic fun initMap() {}

    @JvmStatic fun isJavaIdentifierPart(key: Char): Boolean {
        val i = key.code
        return (bitsIsPart[i shr 5] and (1 shl (i and 31))) != 0
    }

    @JvmStatic fun isJavaIdentifierStart(key: Char): Boolean {
        val i = key.code
        return (bitsIsStart[i shr 5] and (1 shl (i and 31))) != 0
    }

    @JvmStatic fun couldBeEmoji(cp: Int) = cp in 0x1F000..0x1FAFF
    @JvmStatic fun isFitzpatrick(cp: Int) = cp in 0x1F3FB..0x1F3FF
    @JvmStatic fun isZWJ(cp: Int) = cp == 0x200D
    @JvmStatic fun isZWNJ(cp: Int) = cp == 0x200C
    @JvmStatic fun isVariationSelector(cp: Int) = cp == 0xFE0E || cp == 0xFE0F
    @JvmStatic fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z'
}
