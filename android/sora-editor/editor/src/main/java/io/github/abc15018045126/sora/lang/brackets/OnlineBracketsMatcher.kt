package io.github.abc15018045126.sora.lang.brackets

import io.github.abc15018045126.sora.text.Content


class OnlineBracketsMatcher(
    private val pairs: CharArray,
    private val limit: Int
) : BracketsProvider {

    init {
        if ((pairs.size and 1) != 0) {
            throw IllegalArgumentException("pairs must have even length")
        }
    }

    private fun findIndex(ch: Char): Int {
        for (i in pairs.indices) {
            if (ch == pairs[i]) {
                return i
            }
        }
        return -1
    }

    private fun tryComputePaired(text: Content, index: Int): PairedBracket? {
        if (index < 0 || index >= text.length) return null
        val startPos = text.getIndexer().getCharPosition(index)
        val a = text.charAt(startPos.line, startPos.column)
        val symbolIndex = findIndex(a)
        if (symbolIndex == -1) return null

        val b = pairs[symbolIndex xor 1]
        var stack = 0

        if ((symbolIndex and 1) == 0) {

            var currentLineIndex = startPos.line
            var currentCol = startPos.column + 1
            var absIndex = index + 1

            while (currentLineIndex < text.lineCount && absIndex - index < limit) {
                val lineObj = text.getLine(currentLineIndex)
                val len = lineObj.length
                val sepLen = lineObj.lineSeparatorSafe.length


                while (currentCol < len && absIndex - index < limit) {
                    val ch = lineObj[currentCol]
                    if (ch == b) {
                        if (stack <= 0) return PairedBracket(leftIndex = index, rightIndex = absIndex)
                        stack--
                    } else if (ch == a) {
                        stack++
                    }
                    currentCol++
                    absIndex++
                }


                if (currentCol >= len) {
                    val sepCols = currentCol - len
                    if (sepCols < sepLen) {

                         val sepText = lineObj.lineSeparatorSafe.content
                         for (k in sepCols until sepLen) {
                             if (absIndex - index >= limit) break
                             val ch = sepText[k]
                             if (ch == b) {
                                 if (stack <= 0) return PairedBracket(leftIndex = index, rightIndex = absIndex)
                                 stack--
                             } else if (ch == a) {
                                 stack++
                             }
                             absIndex++
                         }
                    }

                    currentLineIndex++
                    currentCol = 0
                }
            }
        } else {

            var currentLineIndex = startPos.line
            var currentCol = startPos.column - 1
            var absIndex = index - 1

            while (currentLineIndex >= 0 && index - absIndex < limit) {
                val lineObj = text.getLine(currentLineIndex)








                val len = lineObj.length
                val sepLen = lineObj.lineSeparatorSafe.length
                val totalLen = len + sepLen


                if (currentCol >= totalLen) currentCol = totalLen - 1

                if (currentCol >= len) {

                    val sepText = lineObj.lineSeparatorSafe.content
                    while (currentCol >= len && index - absIndex < limit) {
                        val ch = sepText[currentCol - len]
                        if (ch == b) {
                            if (stack <= 0) return PairedBracket(leftIndex = absIndex, rightIndex = index)
                            stack--
                        } else if (ch == a) {
                            stack++
                        }
                        currentCol--
                        absIndex--
                    }
                }


                while (currentCol >= 0 && index - absIndex < limit) {
                    val ch = lineObj[currentCol]
                     if (ch == b) {
                        if (stack <= 0) return PairedBracket(leftIndex = absIndex, rightIndex = index)
                        stack--
                    } else if (ch == a) {
                        stack++
                    }
                    currentCol--
                    absIndex--
                }

                currentLineIndex--
                if (currentLineIndex >= 0) {

                     val prevLine = text.getLine(currentLineIndex)
                     currentCol = prevLine.length + prevLine.lineSeparatorSafe.length - 1
                }
            }
        }
        return null
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        var pairedBracket: PairedBracket? = null
        if (index > 0) {
            pairedBracket = tryComputePaired(text, index - 1)
        }
        if (pairedBracket == null && index < text.length) {
            pairedBracket = tryComputePaired(text, index)
        }
        return pairedBracket
    }
}
