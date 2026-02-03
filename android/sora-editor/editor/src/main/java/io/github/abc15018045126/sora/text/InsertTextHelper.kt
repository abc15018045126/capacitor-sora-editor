package io.github.abc15018045126.sora.text

internal class InsertTextHelper {
    private var text: CharSequence? = null
    var index = 0; private set
    var indexNext = 0; private set
    private var length = 0

    private fun init(t: CharSequence) {
        text = t
        index = -1
        indexNext = 0
        length = t.length
    }

    fun forward(): Int {
        index = indexNext
        if (index == length) return TYPE_EOF
        val t = text!!
        when (val ch = t[index]) {
            '\n' -> { indexNext = index + 1; return TYPE_NEWLINE }
            '\r' -> {
                indexNext = if (index + 1 < length && t[index + 1] == '\n') index + 2 else index + 1
                return TYPE_NEWLINE
            }
            else -> {
                indexNext = index + 1
                while (indexNext < length) {
                    val nextCh = t[indexNext]
                    if (nextCh == '\n' || nextCh == '\r') break
                    indexNext++
                }
                return TYPE_LINE_CONTENT
            }
        }
    }

    fun recycle() = synchronized(sCached) {
        for (i in sCached.indices) if (sCached[i] == null) {
            sCached[i] = this
            text = null; index = 0; length = 0
            break
        }
    }

    companion object {
        private val sCached = arrayOfNulls<InsertTextHelper>(8)
        const val TYPE_LINE_CONTENT = 0
        const val TYPE_NEWLINE = 1
        const val TYPE_EOF = 2

        private fun obtain() = synchronized(sCached) {
            for (i in sCached.indices) if (sCached[i] != null) {
                return@synchronized sCached[i].also { sCached[i] = null }!!
            }
            InsertTextHelper()
        }

        @JvmStatic
        fun forInsertion(text: CharSequence) = obtain().apply { init(text) }
    }
}
