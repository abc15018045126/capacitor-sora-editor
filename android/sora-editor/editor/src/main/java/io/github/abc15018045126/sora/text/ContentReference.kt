package io.github.abc15018045126.sora.text

import java.io.IOException
import java.io.Reader
import kotlin.math.max
import kotlin.math.min


class ContentReference(private val content: Content) : TextReference(content) {

    override fun get(index: Int): Char {
        validateAccess()
        return content[index]
    }

    fun charAt(line: Int, column: Int): Char {
        validateAccess()
        return content.getLine(line)[column]
    }

    fun getCharIndex(line: Int, column: Int): Int {
        validateAccess()
        return content.getCharIndex(line, column)
    }

    fun getCharPosition(line: Int, column: Int): CharPosition {
        validateAccess()
        return content.getIndexer().getCharPosition(line, column)
    }

    fun getCharPosition(index: Int): CharPosition {
        validateAccess()
        return content.getIndexer().getCharPosition(index)
    }


    val lineCount: Int
        get() {
            validateAccess()
            return content.lineCount
        }


    fun getColumnCount(line: Int): Int {
        validateAccess()
        return content.getColumnCount(line)
    }


    fun getLineSeparator(line: Int): String {
        validateAccess()
        return content.getLineSeparatorUnsafe(line).content
    }


    fun getLine(line: Int): String {
        validateAccess()
        return content.getLineString(line)
    }


    fun getLineChars(line: Int, dest: CharArray) {
        validateAccess()
        content.getLineChars(line, dest)
    }


    fun appendLineTo(sb: StringBuilder, line: Int) {
        validateAccess()
        content.getLine(line).appendTo(sb)
    }


    val documentVersionValue: Long
        get() {
            validateAccess()
            return content.documentVersion.get()
        }


    fun createReader(): Reader {
        return RefReader()
    }

    override val reference: Content
        get() = super.reference as Content

    override fun setValidator(validator: Validator?): ContentReference {
        super.setValidator(validator)
        return this
    }

    private inner class RefReader : Reader() {

        private var markedLine = 0
        private var markedColumn = 0
        private var line = 0
        private var column = 0

        override fun read(chars: CharArray, offset: Int, length: Int): Int {
            if (chars.size < offset + length) {
                throw IllegalArgumentException("size not enough")
            }
            var read = 0
            while (read < length && line < lineCount) {
                val targetLine = content.getLine(line)
                val lineSep = targetLine.lineSeparatorSafe
                val separatorLength = lineSep.length
                val columnCount = targetLine.length
                var toRead = min(columnCount - column, length - read)
                toRead = max(0, toRead)
                if (toRead > 0) {
                    content.getRegionOnLine(line, column, column + toRead, chars, offset + read)
                }
                column += toRead
                read += toRead
                while (read < length && columnCount <= column && column < columnCount + separatorLength) {
                    chars[offset + read] = lineSep.content[column - columnCount]
                    read++
                    column++
                }
                if (column >= columnCount + separatorLength) {
                    line++
                    column = 0
                }
            }
            if (read == 0) {
                return -1
            }
            return read
        }

        override fun close() {
            line = Int.MAX_VALUE
            column = Int.MAX_VALUE
        }

        override fun markSupported(): Boolean {
            return true
        }

        @Throws(IOException::class)
        override fun mark(readAheadLimit: Int) {
            markedLine = line
            markedColumn = column
        }

        @Throws(IOException::class)
        override fun reset() {
            line = markedLine
            column = markedColumn
        }
    }
}
