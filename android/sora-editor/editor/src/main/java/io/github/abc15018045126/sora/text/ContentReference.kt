package io.github.abc15018045126.sora.text

import java.io.Reader
import kotlin.math.max
import kotlin.math.min

class ContentReference(private val content: Content) : TextReference(content) {
    override fun get(index: Int) = validateAccess().let { content[index] }
    fun charAt(line: Int, column: Int) = validateAccess().let { content.getLine(line)[column] }
    fun getCharIndex(line: Int, column: Int) = validateAccess().let { content.getCharIndex(line, column) }
    fun getCharPosition(line: Int, column: Int) = validateAccess().let { content.indexer.getCharPosition(line, column) }
    fun getCharPosition(index: Int) = validateAccess().let { content.indexer.getCharPosition(index) }

    val lineCount: Int get() = validateAccess().let { content.lineCount }
    fun getColumnCount(line: Int) = validateAccess().let { content.getColumnCount(line) }
    fun getLineSeparator(line: Int) = validateAccess().let { content.getLineSeparatorUnsafe(line).content }
    fun getLine(line: Int) = validateAccess().let { content.getLineString(line) }
    fun getLineChars(line: Int, dest: CharArray) = validateAccess().let { content.getLineChars(line, dest) }
    fun appendLineTo(sb: StringBuilder, line: Int) = validateAccess().let { content.getLine(line).appendTo(sb) }
    val documentVersionValue: Long get() = validateAccess().let { content.documentVersion.get() }
    fun createReader(): Reader = RefReader()
    override val reference: Content get() = super.reference as Content
    override fun setValidator(validator: Validator?) = super.setValidator(validator).let { this }

    private inner class RefReader : Reader() {
        private var markedLine = 0; private var markedColumn = 0
        private var line = 0; private var column = 0

        override fun read(chars: CharArray, offset: Int, length: Int): Int {
            if (chars.size < offset + length) throw IllegalArgumentException("size not enough")
            var read = 0
            while (read < length && line < lineCount) {
                val targetLine = content.getLine(line)
                val lineSep = targetLine.lineSeparatorSafe
                val sepLen = lineSep.length
                val colCount = targetLine.length
                val toRead = max(0, min(colCount - column, length - read))
                if (toRead > 0) content.getRegionOnLine(line, column, column + toRead, chars, offset + read)
                column += toRead; read += toRead
                while (read < length && column in colCount until colCount + sepLen) {
                    chars[offset + read++] = lineSep.content[column++ - colCount]
                }
                if (column >= colCount + sepLen) { line++; column = 0 }
            }
            return if (read == 0) -1 else read
        }

        override fun close() { line = Int.MAX_VALUE; column = Int.MAX_VALUE }
        override fun markSupported() = true
        override fun mark(limit: Int) { markedLine = line; markedColumn = column }
        override fun reset() { line = markedLine; column = markedColumn }
    }
}
