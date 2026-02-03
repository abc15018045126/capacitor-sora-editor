package io.github.abc15018045126.sora.text


enum class LineSeparator(val content: String) {

    LF("\n"),


    CR("\r"),


    CRLF("\r\n"),


    NONE("");

    val length: Int
        get() = content.length

    val chars: CharArray
        get() = content.toCharArray()

    companion object {
        @JvmStatic
        fun fromSeparatorString(text: CharSequence, start: Int, end: Int): LineSeparator {
            val len = end - start
            if (len == 1) {
                val ch = text[start]
                if (ch == '\n') return LF
                if (ch == '\r') return CR
            } else if (len == 2) {
                if (text[start] == '\r' && text[start + 1] == '\n') {
                    return CRLF
                }
            }
            return NONE
        }
    }
}
