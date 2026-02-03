package io.github.abc15018045126.sora.util

import java.util.Arrays


object MyCharacter {


    private var bitsIsStart: IntArray? = null


    private var bitsIsPart: IntArray? = null

    init {
        initMapInternal()
    }


    private fun get(values: IntArray?, bitIndex: Int): Boolean {
        return (values!![bitIndex / 32] and (1 shl (bitIndex % 32))) != 0
    }


    private fun set(values: IntArray?, bitIndex: Int) {
        values!![bitIndex / 32] = values[bitIndex / 32] or (1 shl (bitIndex % 32))
    }


    @Deprecated("The class will be initialized automatically")
    @JvmStatic
    fun initMap() {

    }


    private fun initMapInternal() {
        if (bitsIsStart != null) {
            return
        }
        bitsIsPart = IntArray(2048)
        bitsIsStart = IntArray(2048)
        Arrays.fill(bitsIsPart!!, 0)
        Arrays.fill(bitsIsStart!!, 0)
        for (i in 0..65535) {
            if (Character.isJavaIdentifierPart(i.toChar())) {
                set(bitsIsPart, i)
            }
            if (Character.isJavaIdentifierStart(i.toChar())) {
                set(bitsIsStart, i)
            }
        }
    }


    @JvmStatic
    fun isJavaIdentifierPart(key: Char): Boolean {
        val i = key.code
        return (bitsIsPart!![i shr 5] and (1 shl (i and 31))) != 0
    }


    @JvmStatic
    fun isJavaIdentifierStart(key: Char): Boolean {
        val i = key.code
        return (bitsIsStart!![i shr 5] and (1 shl (i and 31))) != 0
    }

    @JvmStatic
    fun couldBeEmoji(cp: Int): Boolean {
        return cp in 0x1F000..0x1FAFF
    }

    @JvmStatic
    fun isFitzpatrick(cp: Int): Boolean {
        return cp in 0x1F3FB..0x1F3FF
    }

    @JvmStatic
    fun isZWJ(cp: Int): Boolean {
        return cp == 0x200D
    }

    @JvmStatic
    fun isZWNJ(cp: Int): Boolean {
        return cp == 0x200C
    }

    @JvmStatic
    fun isVariationSelector(cp: Int): Boolean {
        return cp == 0xFE0E || cp == 0xFE0F
    }

    @JvmStatic
    fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }
}
