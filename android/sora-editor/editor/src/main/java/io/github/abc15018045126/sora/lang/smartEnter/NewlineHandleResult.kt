package io.github.abc15018045126.sora.lang.smartEnter

class NewlineHandleResult(

    @JvmField val text: CharSequence,

    @JvmField val shiftLeft: Int
) {
    init {
        if (shiftLeft < 0 || shiftLeft > text.length) {
            throw IllegalArgumentException("invalid shiftLeft")
        }
    }
}
