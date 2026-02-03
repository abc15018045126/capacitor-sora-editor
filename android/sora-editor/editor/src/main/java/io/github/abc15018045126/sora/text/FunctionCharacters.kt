package io.github.abc15018045126.sora.text

object FunctionCharacters {
    private val names = arrayOf(
        "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL", "BS", "HT", "LF", "VT", "FF", "CR", "SO", "SI",
        "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB", "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US", "SP"
    )

    @JvmStatic fun isFunctionCharacter(c: Char) = c.code < 32 || c.code == 127
    @JvmStatic fun isEditorFunctionChar(c: Char) = c != '\t' && isFunctionCharacter(c)

    @JvmStatic fun getNameForFunctionCharacter(c: Char) = when {
        c.code < 32 -> names[c.code]
        c.code == 127 -> "DEL"
        else -> "UNK"
    }
}
