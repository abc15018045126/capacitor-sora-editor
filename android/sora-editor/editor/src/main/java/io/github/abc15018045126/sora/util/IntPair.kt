package io.github.abc15018045126.sora.util


object IntPair {


    private fun toUnsignedLong(x: Int): Long {
        return x.toLong() and 0xffffffffL
    }


    @JvmStatic
    fun pack(first: Int, second: Int): Long {
        return (toUnsignedLong(first) shl 32) or toUnsignedLong(second)
    }


    @JvmStatic
    fun getSecond(packedValue: Long): Int {
        return (packedValue and 0xFFFFFFFFL).toInt()
    }


    @JvmStatic
    fun getFirst(packedValue: Long): Int {
        return (packedValue ushr 32).toInt()
    }


    @JvmStatic
    fun packIntFloat(first: Int, second: Float): Long {
        return pack(first, java.lang.Float.floatToRawIntBits(second))
    }


    @JvmStatic
    fun getSecondAsFloat(packedValue: Long): Float {
        return java.lang.Float.intBitsToFloat(getSecond(packedValue))
    }
}
