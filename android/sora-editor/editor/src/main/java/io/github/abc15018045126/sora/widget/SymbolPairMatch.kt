package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine


open class SymbolPairMatch(var parent: SymbolPairMatch? = null) {

    private val singleCharPairMaps = mutableMapOf<Char, SymbolPair?>()
    private val multipleCharByEndPairMaps = mutableMapOf<Char, MutableList<SymbolPair?>>()


    fun putPair(singleCharacter: Char, symbolPair: SymbolPair?) {
        singleCharPairMaps[singleCharacter] = symbolPair
    }


    fun putPair(charArray: CharArray, symbolPair: SymbolPair?) {
        val endChar = charArray[charArray.size - 1]
        multipleCharByEndPairMaps.getOrPut(endChar) { mutableListOf() }.add(symbolPair)
    }


    fun putPair(openString: String, symbolPair: SymbolPair?) {
        putPair(openString.toCharArray(), symbolPair)
    }

    fun matchBestPairBySingleChar(editChar: Char): SymbolPair? {
        val pair = singleCharPairMaps[editChar]
        if (pair == null && parent != null) {
            return parent!!.matchBestPairBySingleChar(editChar)
        }
        return pair
    }

    fun matchBestPairList(editChar: Char): List<SymbolPair?> {
        var result = multipleCharByEndPairMaps[editChar]
        if (result == null && parent != null) {
            val parentResult = parent!!.matchBestPairList(editChar)
            result = parentResult.toMutableList()
        }
        return result ?: emptyList()
    }

    fun matchBestPair(
        editor: CodeEditor,
        cursorPosition: CharPosition,
        inputCharArray: CharArray?,
        endChar: Char
    ): SymbolPair? {
        val content = editor.text

        val singleCharPair = if (inputCharArray == null) matchBestPairBySingleChar(endChar) else null


        if (singleCharPair != null) {
            singleCharPair.measureCursorPosition(cursorPosition.index)
            return singleCharPair
        }


        val matchList = matchBestPairList(endChar)

        var matchPair: SymbolPair? = null
        for (pair in matchList) {
            if (pair == null || !pair.shouldReplace(editor)) {
                continue
            }
            val openCharArray = pair.open.toCharArray()


            var matchFlag = 1
            var insertIndex = cursorPosition.index


            if (inputCharArray == null) {
                var arrayIndex = openCharArray.size - 2
                while (arrayIndex >= 0) {
                    if (insertIndex > 0) {
                        insertIndex--
                    }
                    val contentChar = content.get(insertIndex)
                    matchFlag = if (contentChar == openCharArray[arrayIndex]) matchFlag else 0
                    arrayIndex--
                }
            } else {






                if (inputCharArray.size > openCharArray.size) {
                    continue
                }

                var pairIndex = openCharArray.size - 1

                for (charIndex in inputCharArray.size - 1 downTo 1) {
                    matchFlag = if (inputCharArray[charIndex] == openCharArray[pairIndex]) matchFlag else 0
                    pairIndex--
                }


                if (matchFlag == 1 && pairIndex > 0) {




                    insertIndex--

                    while (pairIndex >= 0) {
                        matchFlag = if (content.get(insertIndex) == openCharArray[pairIndex]) matchFlag else 0
                        insertIndex--
                        pairIndex--
                    }
                }
            }

            if (matchFlag == 1) {
                matchPair = pair
                pair.measureCursorPosition(insertIndex)
                break
            }
        }
        return matchPair
    }

    fun removeAllPairs() {
        singleCharPairMaps.clear()
        multipleCharByEndPairMaps.clear()
    }


    open class SymbolPair {
        @JvmField
        val open: String
        @JvmField
        val close: String
        private var symbolPairEx: SymbolPairEx? = null
        var cursorOffset = 0
            private set
        var insertOffset = 0
            private set


        constructor(open: String, close: String) {
            this.open = open
            this.close = close
        }

        constructor(open: String, close: String, symbolPairEx: SymbolPairEx?) : this(open, close) {
            this.symbolPairEx = symbolPairEx
        }

        open fun shouldReplace(editor: CodeEditor): Boolean {
            val ex = symbolPairEx ?: return true
            val content = editor.text
            val currentLine = content.getLine(editor.cursor?.leftLine ?: 0)
            return ex.shouldReplace(editor, currentLine, editor.cursor?.leftColumn ?: 0)

        }

        fun shouldDoAutoSurround(content: Content): Boolean {
            val ex = symbolPairEx ?: return false
            return ex.shouldDoAutoSurround(content)
        }

        fun measureCursorPosition(offsetIndex: Int) {
            cursorOffset = offsetIndex + open.length
            insertOffset = offsetIndex
        }


        interface SymbolPairEx {

            fun shouldReplace(editor: CodeEditor, currentLine: ContentLine, leftColumn: Int): Boolean {
                return true
            }


            fun shouldDoAutoSurround(content: Content): Boolean {
                return false
            }
        }

        companion object {

            @JvmField
            val EMPTY_SYMBOL_PAIR = SymbolPair("", "")
        }
    }

    class DefaultSymbolPairs : SymbolPairMatch() {
        init {
            putPair('{', SymbolPair("{", "}"))
            putPair('(', SymbolPair("(", ")"))
            putPair('[', SymbolPair("[", "]"))
            putPair('"', SymbolPair("\"", "\"", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected()
                }
            }))
            putPair('\'', SymbolPair("'", "'", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected()
                }
            }))
        }
    }
}
