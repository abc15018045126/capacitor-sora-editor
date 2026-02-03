package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine

open class SymbolPairMatch(var parent: SymbolPairMatch? = null) {
    private val singleCharPairMaps = mutableMapOf<Char, SymbolPair?>()
    private val multipleCharByEndPairMaps = mutableMapOf<Char, MutableList<SymbolPair?>>()

    fun putPair(singleCharacter: Char, symbolPair: SymbolPair?) { singleCharPairMaps[singleCharacter] = symbolPair }
    fun putPair(charArray: CharArray, symbolPair: SymbolPair?) = multipleCharByEndPairMaps.getOrPut(charArray.last()) { mutableListOf() }.add(symbolPair)
    fun putPair(openString: String, symbolPair: SymbolPair?) = putPair(openString.toCharArray(), symbolPair)

    fun matchBestPairBySingleChar(editChar: Char): SymbolPair? = singleCharPairMaps[editChar] ?: parent?.matchBestPairBySingleChar(editChar)

    fun matchBestPairList(editChar: Char): List<SymbolPair?> = (multipleCharByEndPairMaps[editChar] ?: parent?.matchBestPairList(editChar)?.toMutableList()) ?: emptyList()

    fun matchBestPair(editor: CodeEditor, cursorPosition: CharPosition, inputCharArray: CharArray?, endChar: Char): SymbolPair? {
        if (inputCharArray == null) {
            matchBestPairBySingleChar(endChar)?.let {
                it.measureCursorPosition(cursorPosition.index)
                return it
            }
        }
        val content = editor.text
        for (pair in matchBestPairList(endChar)) {
            if (pair == null || !pair.shouldReplace(editor)) continue
            val openCharArray = pair.open.toCharArray()
            var match = true
            var insertIndex = cursorPosition.index

            if (inputCharArray == null) {
                for (i in openCharArray.lastIndex - 1 downTo 0) {
                    if (insertIndex > 0) insertIndex--
                    if (content.get(insertIndex) != openCharArray[i]) { match = false; break }
                }
            } else {
                if (inputCharArray.size > openCharArray.size) continue
                var pairIndex = openCharArray.lastIndex
                for (charIndex in inputCharArray.lastIndex downTo 1) {
                    if (inputCharArray[charIndex] != openCharArray[pairIndex--]) { match = false; break }
                }
                if (match && pairIndex > 0) {
                    insertIndex--
                    while (pairIndex >= 0) {
                        if (content.get(insertIndex--) != openCharArray[pairIndex--]) { match = false; break }
                    }
                }
            }
            if (match) {
                pair.measureCursorPosition(insertIndex)
                return pair
            }
        }
        return null
    }

    fun removeAllPairs() {
        singleCharPairMaps.clear()
        multipleCharByEndPairMaps.clear()
    }

    open class SymbolPair(
        @JvmField val open: String,
        @JvmField val close: String,
        private var symbolPairEx: SymbolPairEx? = null
    ) {
        var cursorOffset = 0
            private set
        var insertOffset = 0
            private set

        open fun shouldReplace(editor: CodeEditor): Boolean {
            val ex = symbolPairEx ?: return true
            return ex.shouldReplace(editor, editor.text.getLine(editor.cursor?.leftLine ?: 0), editor.cursor?.leftColumn ?: 0)
        }

        fun shouldDoAutoSurround(content: Content) = symbolPairEx?.shouldDoAutoSurround(content) ?: false

        fun measureCursorPosition(offsetIndex: Int) {
            cursorOffset = offsetIndex + open.length
            insertOffset = offsetIndex
        }

        interface SymbolPairEx {
            fun shouldReplace(editor: CodeEditor, currentLine: ContentLine, leftColumn: Int) = true
            fun shouldDoAutoSurround(content: Content) = false
        }

        companion object {
            @JvmField val EMPTY_SYMBOL_PAIR = SymbolPair("", "")
        }
    }

    class DefaultSymbolPairs : SymbolPairMatch() {
        init {
            putPair('{', SymbolPair("{", "}"))
            putPair('(', SymbolPair("(", ")"))
            putPair('[', SymbolPair("[", "]"))
            val quoteEx = object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content) = content.cursor.isSelected()
            }
            putPair('"', SymbolPair("\"", "\"", quoteEx))
            putPair('\'', SymbolPair("'", "'", quoteEx))
        }
    }
}
