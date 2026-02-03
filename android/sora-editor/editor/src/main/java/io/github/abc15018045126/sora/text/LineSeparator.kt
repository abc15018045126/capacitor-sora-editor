package io.github.abc15018045126.sora.text

enum class LineSeparator(val content: String) {
    LF("\n"), CR("\r"), CRLF("\r\n"), NONE("");

    val length get() = content.length
    val chars get() = content.toCharArray()

    companion object {
        @JvmStatic
        fun fromSeparatorString(s: CharSequence, start: Int, end: Int): LineSeparator {
            val len = end - start
            if (len == 1) {
                if (s[start] == '\n') return LF
                if (s[start] == '\r') return CR
            } else if (len == 2 && s[start] == '\r' && s[start + 1] == '\n') return CRLF
            return NONE
        }
    }
}
