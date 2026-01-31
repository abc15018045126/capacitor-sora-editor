
package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine

/**
 * Define symbol pairs to complete them automatically when the user
 * enters the first character of pair.
 *
 * @author abc15018045126
 */
open class SymbolPairMatch(protected var parent: SymbolPairMatch? = null) {

    private val singleCharPairMaps = mutableMapOf<Char, SymbolPair?>()
    private val multipleCharByEndPairMaps = mutableMapOf<Char, MutableList<SymbolPair>>()

    /**
     * Put a pair of symbol completion
     * When the user types the [singleCharacter], it will be replaced by [symbolPair]
     * SymbolPair maybe null to disable completion for this character.
     *
     * @see SymbolPair
     */
    fun putPair(singleCharacter: Char, symbolPair: SymbolPair?) {
        singleCharPairMaps[singleCharacter] = symbolPair
    }

    /**
     * Put a pair of symbol completion
     * When the user types the [charArray], it will be replaced by [symbolPair]
     * SymbolPair maybe null to disable completion for this character.
     *
     * @see SymbolPair
     */
    fun putPair(charArray: CharArray, symbolPair: SymbolPair) {
        val endChar = charArray[charArray.size - 1]
        val list = multipleCharByEndPairMaps.getOrPut(endChar) { mutableListOf() }
        list.add(symbolPair)
    }

    /**
     * Put a pair of symbol completion
     * When the user types the [openString], it will be replaced by [symbolPair]
     * SymbolPair maybe null to disable completion for this character.
     *
     * @see putPair
     */
    fun putPair(openString: String, symbolPair: SymbolPair) {
        putPair(openString.toCharArray(), symbolPair)
    }

    fun matchBestPairBySingleChar(editChar: Char): SymbolPair? {
        val pair = singleCharPairMaps[editChar]
        if (pair == null && parent != null) {
            return parent?.matchBestPairBySingleChar(editChar)
        }
        return pair
    }

    fun matchBestPairList(editChar: Char): List<SymbolPair> {
        val result = multipleCharByEndPairMaps[editChar]
        if (result == null && parent != null) {
            return parent?.matchBestPairList(editChar) ?: emptyList()
        }
        return result ?: emptyList()
    }

    fun matchBestPair(editor: CodeEditor, cursorPosition: CharPosition, inputCharArray: CharArray?, endChar: Char): SymbolPair? {
        val content = editor.text
        // do not apply single character pairs for text with length > 1
        val singleCharPair = if (inputCharArray == null) matchBestPairBySingleChar(endChar) else null

        // matches single character symbol pair first
        if (singleCharPair != null) {
            singleCharPair.measureCursorPosition(cursorPosition.index)
            return singleCharPair
        }

        // find all possible lists, with a single character for fast search
        val matchList = matchBestPairList(endChar)

        var matchPair: SymbolPair? = null
        for (pair in matchList) {
            if (!pair.shouldReplace(editor)) {
                continue
            }
            val openCharArray = pair.open.toCharArray()

            // if flag is not 1, no match
            var matchFlag = 1
            var insertIndex = cursorPosition.index

            // the size = 1, we need compare characters before cursor, ensure it match the whole open char array
            if (inputCharArray == null) {
                var arrayIndex = openCharArray.size - 2
                while (arrayIndex >= 0) {
                    if (insertIndex > 0) {
                        insertIndex--
                    }
                    val contentChar = content[insertIndex]
                    matchFlag = if (contentChar == openCharArray[arrayIndex]) (matchFlag and 1) else 0
                    arrayIndex--
                }
            } else {
                // Not fully tested.

                // Not all the time the user will enter a string that matches the symbol pair,
                // such as pasting text,
                // so if the length of the entered string is greater than the length of the symbol pair,
                // the two are considered to be mismatched
                if (inputCharArray.size > openCharArray.size) {
                    continue
                }

                var pairIndex = openCharArray.size - 1

                for (charIndex in inputCharArray.size - 1 downTo 1) {
                    matchFlag = if (inputCharArray[charIndex] == openCharArray[pairIndex]) (matchFlag and 1) else 0
                    pairIndex--
                }

                // input text and symbol pair text not equal fully, continue compare characters before cursor
                if (matchFlag == 1 && pairIndex > 0) {
                    // When the loop is stopped the character position
                    // is still in the first position of the matched characters,
                    // we need to replace this character,
                    // so we need to subtract a character position
                    insertIndex--

                    while (pairIndex >= 0) {
                        matchFlag = if (content[insertIndex] == openCharArray[pairIndex]) (matchFlag and 1) else 0
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

    /**
     * Defines a replacement of input
     */
    open class SymbolPair {

        @JvmField
        val open: String
        @JvmField
        val close: String
        private var symbolPairEx: SymbolPairEx? = null
        private var cursorOffset: Int = 0
        private var insertOffset: Int = 0

        /**
         * If your [open] string and [close] string are both ', it makes a pair of single quotes.
         * This will replace the entered character with a pair of single quotes,
         * and will move the cursor to the middle of the pair.
         * This class defines these symbol pairs
         */
        constructor(open: String, close: String) {
            this.open = open
            this.close = close
        }

        constructor(open: String, close: String, symbolPairEx: SymbolPairEx?) : this(open, close) {
            this.symbolPairEx = symbolPairEx
        }

        fun shouldReplace(editor: CodeEditor): Boolean {
            val ex = symbolPairEx ?: return true
            val content = editor.text
            val currentLine = content.getLine(content.cursor.leftLine)
            return ex.shouldReplace(editor, currentLine, content.cursor.leftColumn)
        }

        fun shouldDoAutoSurround(content: Content): Boolean {
            val ex = symbolPairEx ?: return false
            return ex.shouldDoAutoSurround(content)
        }

        internal fun measureCursorPosition(offsetIndex: Int) {
            cursorOffset = offsetIndex + open.length
            insertOffset = offsetIndex
        }

        fun getCursorOffset(): Int {
            return cursorOffset
        }

        fun getInsertOffset(): Int {
            return insertOffset
        }

        interface SymbolPairEx {
            /**
             * The method will be called
             * to decide whether to perform the replacement or not.
             * If not implemented, always return true
             */
            fun shouldReplace(editor: CodeEditor, currentLine: ContentLine, leftColumn: Int): Boolean = true

            /**
             * when before the replaced and select a range,surrounds the selected content with return ture.
             * If not implemented, always return false
             */
            fun shouldDoAutoSurround(content: Content): Boolean = false
        }

        companion object {
            /**
             * Defines that this character does not have to be replaced
             */
            @JvmField
            val EMPTY_SYMBOL_PAIR = SymbolPair("", "")
        }
    }

    open class DefaultSymbolPairs : SymbolPairMatch {
        constructor() : super() {
            putPair('{', SymbolPair("{", "}"))
            putPair('(', SymbolPair("(", ")"))
            putPair('[', SymbolPair("[", "]"))
            putPair('"', SymbolPair("\"", "\"", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected
                }
            }))
            putPair('\'', SymbolPair("'", "'", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected
                }
            }))
        }
    }
}
