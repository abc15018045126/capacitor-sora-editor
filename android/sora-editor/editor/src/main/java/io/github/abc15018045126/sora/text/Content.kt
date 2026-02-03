package io.github.abc15018045126.sora.text

import io.github.abc15018045126.sora.text.bidi.ContentBidi
import io.github.abc15018045126.sora.text.bidi.Directions
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

open class Content(src: CharSequence?, threadSafe: Boolean) : CharSequence {
    constructor() : this(null)
    constructor(src: CharSequence?) : this(src, true)

    internal val lines = ArrayList<ContentLine>(initialLineCapacity).apply { add(ContentLine()) }
    private val contentListeners = ArrayList<ContentListener>()
    private val lock: ReadWriteLock? = if (threadSafe) ReentrantReadWriteLock() else null
    private var textLength = 0
    private var nestedBatchEdit = 0
    internal val documentVersion = AtomicLong(1L)
    internal var indexer: Indexer = CachedIndexer(this)
    private val bidi = ContentBidi(this)
    internal var undoManager = UndoManager()
    private var _cursor: Cursor? = null

    var maxUndoStackSize: Int
        get() = undoManager.maxUndoStackSize
        set(maxSize) { undoManager.maxUndoStackSize = maxSize }

    var isUndoEnabled: Boolean
        get() = undoManager.isUndoEnabled
        set(enabled) { undoManager.isUndoEnabled = enabled }

    init {
        val finalSrc = src ?: ""
        maxUndoStackSize = DEFAULT_MAX_UNDO_STACK_SIZE
        if (finalSrc.isNotEmpty()) {
            isUndoEnabled = false
            insert(0, 0, finalSrc)
            isUndoEnabled = true
        } else isUndoEnabled = true
    }

    val isThreadSafe get() = lock != null

    internal fun lock(write: Boolean) {
        if (lock == null) return
        if (write) lock.writeLock().lock() else lock.readLock().lock()
    }

    internal fun unlock(write: Boolean) {
        if (lock == null) return
        if (write) lock.writeLock().unlock() else lock.readLock().unlock()
    }

    override fun get(index: Int): Char {
        checkIndex(index, CHECK_TYPE_READ)
        lock(false)
        return try {
            val p = getIndexer().getCharPosition(index)
            lines[p.line][p.column]
        } finally { unlock(false) }
    }

    fun charAt(line: Int, column: Int): Char {
        lock(false)
        return try {
            checkLineAndColumn(line, column, CHECK_TYPE_READ)
            lines[line][column]
        } finally { unlock(false) }
    }

    override val length get() = textLength

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex > endIndex) throw StringIndexOutOfBoundsException("start > end")
        lock(false)
        return try {
            val s = getIndexer().getCharPosition(startIndex)
            val e = getIndexer().getCharPosition(endIndex)
            subContentInternal(s.line, s.column, e.line, e.column, true)
        } finally { unlock(false) }
    }

    fun substring(start: Int, end: Int): String {
        if (start > end) throw StringIndexOutOfBoundsException("start > end")
        lock(false)
        return try {
            val s = getIndexer().getCharPosition(start)
            val e = getIndexer().getCharPosition(end)
            subStringBuilder(s.line, s.column, e.line, e.column, end - start + 1).toString()
        } finally { unlock(false) }
    }

    fun getLine(line: Int): ContentLine {
        lock(false)
        return try { lines[line] } finally { unlock(false) }
    }

    internal fun getLineUnsafe(line: Int) = lines[line]
    val lineCount get() = lines.size
    fun getColumnCount(line: Int) = getLine(line).length
    internal fun getColumnCountUnsafe(line: Int) = lines[line].length
    internal fun getLineSeparatorUnsafe(line: Int) = lines[line].lineSeparatorSafe

    fun getLineString(line: Int): String {
        lock(false)
        return try {
            checkLine(line)
            lines[line].toString()
        } finally { unlock(false) }
    }

    fun getRegionOnLine(line: Int, start: Int, end: Int, dest: CharArray, offset: Int) {
        lock(false)
        try { lines[line].getChars(start, end, dest, offset) } finally { unlock(false) }
    }

    fun getLineChars(line: Int, dest: CharArray) = getRegionOnLine(line, 0, getColumnCount(line), dest, 0)

    fun getCharIndex(line: Int, column: Int): Int {
        lock(false)
        return try { getIndexer().getCharIndex(line, column) } finally { unlock(false) }
    }

    fun isValidPosition(position: CharPosition?): Boolean {
        if (position == null) return false
        val line = position.line
        val column = position.column
        val index = position.index
        lock(false)
        return try {
            if (line < 0 || line >= lineCount) return false
            val text = getLine(line)
            if (column > text.length + text.lineSeparatorSafe.length || column < 0) return false
            getIndexer().getCharIndex(line, column) == index
        } finally { unlock(false) }
    }

    fun insert(line: Int, column: Int, text: CharSequence) {
        lock(true)
        documentVersion.getAndIncrement()
        try { insertInternal(line, column, text) } finally { unlock(true) }
    }

    private fun insertInternal(line: Int, column: Int, text: CharSequence) {
        checkLineAndColumn(line, column, CHECK_TYPE_CURSOR)
        val col = if (column > lines[line].length) lines[line].length else column
        _cursor?.beforeInsert(line, col)
        dispatchBeforeModification()

        var workLine = line
        var workIndex = col
        var currLine = makeLineMutable(workLine)
        val helper = InsertTextHelper.forInsertion(text)
        var type: Int
        var peekType = InsertTextHelper.TYPE_EOF
        var fromPeek = false
        val newLines = LinkedList<ContentLine>()
        val startSeparator = currLine.lineSeparatorSafe

        while (true) {
            type = if (fromPeek) peekType else helper.forward()
            fromPeek = false
            if (type == InsertTextHelper.TYPE_EOF) break
            if (type == InsertTextHelper.TYPE_LINE_CONTENT) {
                currLine.insert(workIndex, text, helper.index, helper.indexNext)
                workIndex += helper.indexNext - helper.index
            } else {
                currLine.lineSeparator = LineSeparator.fromSeparatorString(text, helper.index, helper.indexNext)
                peekType = helper.forward()
                fromPeek = true
                val newLine = ContentLine(currLine.length - workIndex + helper.indexNext - helper.index + 10)
                newLine.insert(0, currLine, workIndex, currLine.length)
                currLine.delete(workIndex, currLine.length)
                workIndex = 0
                currLine = newLine
                newLines.add(newLine)
                workLine++
            }
        }
        currLine.lineSeparator = startSeparator
        lines.addAll(line + 1, newLines)
        helper.recycle()
        textLength += text.length
        dispatchAfterInsert(line, col, workLine, workIndex, text)
    }

    fun delete(start: Int, end: Int) {
        lock(true)
        checkIndex(start, CHECK_TYPE_CURSOR)
        checkIndex(end, CHECK_TYPE_CURSOR)
        documentVersion.getAndIncrement()
        try {
            val startPos = getIndexer().getCharPosition(start)
            val endPos = getIndexer().getCharPosition(end)
            if (start != end) deleteInternal(startPos.line, startPos.column, endPos.line, endPos.column)
        } finally { unlock(true) }
    }

    fun delete(startLine: Int, columnOnStartLine: Int, endLine: Int, columnOnEndLine: Int) {
        lock(true)
        documentVersion.getAndIncrement()
        try { deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine) } finally { unlock(true) }
    }

    private fun deleteInternal(startLine: Int, columnOnStartLine: Int, endLine: Int, columnOnEndLine: Int) {
        checkLineAndColumn(endLine, columnOnEndLine, CHECK_TYPE_CURSOR)
        checkLineAndColumn(startLine, columnOnStartLine, CHECK_TYPE_CURSOR)
        if (startLine == endLine && columnOnStartLine == columnOnEndLine) return

        val endLineObj = lines[endLine]
        if (columnOnEndLine > endLineObj.length && endLine + 1 < lineCount) {
            deleteInternal(startLine, columnOnStartLine, endLine + 1, 0)
            return
        }
        val startLineObj = lines[startLine]
        if (columnOnStartLine > startLineObj.length) {
            deleteInternal(startLine, startLineObj.length, endLine, columnOnEndLine)
            return
        }

        val changedContent = StringBuilder()
        if (startLine == endLine) {
            val curr = makeLineMutable(startLine)
            if (columnOnStartLine < 0 || columnOnEndLine > curr.length || columnOnStartLine > columnOnEndLine) throw StringIndexOutOfBoundsException("invalid bounds")
            _cursor?.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine)
            dispatchBeforeModification()
            changedContent.append(curr, columnOnStartLine, columnOnEndLine)
            curr.delete(columnOnStartLine, columnOnEndLine)
            textLength -= (columnOnEndLine - columnOnStartLine)
        } else if (startLine < endLine) {
            _cursor?.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine)
            dispatchBeforeModification()
            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                val separator = line.lineSeparatorSafe
                textLength -= (line.length + separator.length)
                line.appendTo(changedContent)
                changedContent.append(separator.content)
                line.release()
            }
            if (endLine > startLine + 1) lines.subList(startLine + 1, endLine).clear()
            val currEnd = startLine + 1
            val start = makeLineMutable(startLine)
            val end = lines[currEnd]
            textLength -= (start.length - columnOnStartLine)
            changedContent.insert(0, start.subSequence(columnOnStartLine, start.length))
                .insert(start.length - columnOnStartLine, start.lineSeparatorSafe.content)
            start.delete(columnOnStartLine, start.length)
            textLength -= columnOnEndLine
            changedContent.append(end, 0, columnOnEndLine)
            textLength -= start.lineSeparatorSafe.length
            lines.removeAt(currEnd)
            start.append(TextReference(end, columnOnEndLine, end.length))
            start.lineSeparator = end.lineSeparator
            end.release()
        } else throw IllegalArgumentException("start line > end line")
        dispatchAfterDelete(startLine, columnOnStartLine, endLine, columnOnEndLine, changedContent)
    }

    private fun makeLineMutable(line: Int): ContentLine {
        val data = lines[line]
        val mut = data.toMutable()
        if (mut !== data) {
            lines[line] = mut
            data.release()
        }
        return mut
    }

    fun replace(startLine: Int, columnOnStartLine: Int, endLine: Int, columnOnEndLine: Int, text: CharSequence) {
        lock(true)
        documentVersion.getAndIncrement()
        try {
            dispatchBeforeReplace()
            deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine)
            insertInternal(startLine, columnOnStartLine, text)
        } finally { unlock(true) }
    }

    fun replace(startIndex: Int, endIndex: Int, text: CharSequence) {
        val start = getIndexer().getCharPosition(startIndex)
        val end = getIndexer().getCharPosition(endIndex)
        replace(start.line, start.column, end.line, end.column, text)
    }

    fun getDocumentVersion() = documentVersion.get()
    fun undo() = undoManager.undo(this)
    fun redo() = undoManager.redo(this)
    fun isUndoManagerWorking() = undoManager.isModifyingContent
    fun canUndo() = undoManager.canUndo()
    fun canRedo() = undoManager.canRedo()
    fun beginBatchEdit(): Boolean {
        nestedBatchEdit++
        return isInBatchEdit
    }
    fun endBatchEdit(): Boolean {
        nestedBatchEdit--
        if (nestedBatchEdit == 0) undoManager.onExitBatchEdit()
        if (nestedBatchEdit < 0) nestedBatchEdit = 0
        return isInBatchEdit
    }
    fun getNestedBatchEdit() = nestedBatchEdit
    fun resetBatchEdit() { nestedBatchEdit = 0 }
    val isInBatchEdit get() = nestedBatchEdit > 0

    fun addContentListener(listener: ContentListener) {
        if (listener is Indexer) throw IllegalArgumentException("Permission denied")
        if (!contentListeners.contains(listener)) contentListeners.add(listener)
    }
    fun removeContentListener(listener: ContentListener) {
        if (listener is Indexer) throw IllegalArgumentException("Permission denied")
        contentListeners.remove(listener)
    }

    fun getIndexer() = _cursor?.getIndexer() ?: indexer

    fun subContent(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) = subContent(startLine, startColumn, endLine, endColumn, true)

    fun subContent(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, newContentThreadSafe: Boolean): Content {
        lock(false)
        return try { subContentInternal(startLine, startColumn, endLine, endColumn, newContentThreadSafe) } finally { unlock(false) }
    }

    private fun subContentInternal(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, threadSafe: Boolean): Content {
        val c = Content(null, threadSafe)
        c.isUndoEnabled = false
        if (startLine == endLine) {
            val line = lines[startLine]
            if (endColumn == line.length + 1 && line.lineSeparatorSafe == LineSeparator.CRLF) {
                if (startColumn < endColumn) {
                    c.insert(0, 0, line.subSequence(startColumn, line.length))
                    c.lines[0].lineSeparator = LineSeparator.CR
                    c.textLength++
                    c.lines.add(ContentLine())
                }
            } else c.insert(0, 0, line.subSequence(startColumn, endColumn))
        } else if (startLine < endLine) {
            val firstLine = lines[startLine]
            if (firstLine.lineSeparatorSafe == LineSeparator.CRLF) {
                if (startColumn <= firstLine.length) {
                    c.insert(0, 0, firstLine.subSequence(startColumn, firstLine.length))
                    c.lines[0].lineSeparator = firstLine.lineSeparator
                    c.textLength += firstLine.lineSeparatorSafe.length
                } else if (startColumn == firstLine.length + 1) {
                    c.lines[0].lineSeparator = LineSeparator.LF
                    c.textLength += LineSeparator.LF.length
                } else throw IndexOutOfBoundsException()
            } else {
                c.insert(0, 0, firstLine.subSequence(startColumn, firstLine.length))
                c.lines[0].lineSeparator = firstLine.lineSeparator
                c.textLength += firstLine.lineSeparatorSafe.length
            }
            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                c.lines.add(ContentLine(line))
                c.textLength += line.length + line.lineSeparatorSafe.length
            }
            val end = lines[endLine]
            if (endColumn == end.length + 1 && end.lineSeparatorSafe == LineSeparator.CRLF) {
                val newLine = ContentLine().insert(0, end, 0, endColumn - 1)
                c.lines.add(newLine)
                newLine.lineSeparator = LineSeparator.CR
                c.textLength += endColumn + 1
            } else {
                c.lines.add(ContentLine().insert(0, end, 0, endColumn))
                c.textLength += endColumn
            }
        } else throw StringIndexOutOfBoundsException("start > end")
        c.isUndoEnabled = true
        return c
    }

    private fun subStringBuilder(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, length: Int): StringBuilder {
        val sb = StringBuilder(length)
        if (startLine == endLine) {
            val line = lines[startLine]
            if (endColumn == line.length + 1 && line.lineSeparatorSafe == LineSeparator.CRLF) {
                if (startColumn < endColumn) sb.append(lines[startLine], startColumn, line.length).append(LineSeparator.CR.content)
            } else sb.append(lines[startLine], startColumn, endColumn)
        } else if (startLine < endLine) {
            val firstLine = lines[startLine]
            if (firstLine.lineSeparatorSafe == LineSeparator.CRLF) {
                if (startColumn <= firstLine.length) {
                    sb.append(firstLine, startColumn, firstLine.length)
                    sb.append(firstLine.lineSeparatorSafe.content)
                } else if (startColumn == firstLine.length + 1) {
                    sb.append(LineSeparator.LF.content)
                } else throw IndexOutOfBoundsException()
            } else {
                sb.append(firstLine, startColumn, firstLine.length)
                sb.append(firstLine.lineSeparatorSafe.content)
            }
            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                sb.append(line).append(line.lineSeparatorSafe.content)
            }
            val end = lines[endLine]
            if (endColumn == end.length + 1 && end.lineSeparatorSafe == LineSeparator.CRLF) {
                sb.append(end, 0, endColumn).append(LineSeparator.CR.content)
            } else sb.append(end, 0, endColumn)
        } else throw StringIndexOutOfBoundsException("start > end")
        return sb
    }

    fun getLineDirections(line: Int): Directions {
        lock(false)
        return try { bidi.getLineDirections(lines[line], line) } finally { unlock(false) }
    }

    fun setBidiEnabled(enabled: Boolean) { bidi.isEnabled = enabled }
    fun isBidiEnabled() = bidi.isEnabled

    fun isRtlAt(line: Int, column: Int): Boolean {
        val dirs = getLineDirections(line)
        for (i in 0 until dirs.runCount) {
            if (column >= dirs.getRunStart(i) && column < dirs.getRunEnd(i)) return dirs.isRunRtl(i)
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other is Content) {
            if (other.length != length) return false
            for (i in 0 until lineCount) if (!textEquals(lines[i], other.lines[i])) return false
            return true
        }
        return false
    }

    override fun hashCode() = Objects.hash(lines, textLength)
    override fun toString() = toStringBuilder().toString()
    fun toStringBuilder() = StringBuilder().apply { appendToStringBuilder(this) }
    fun getUndoManager() = undoManager
    fun setUndoManager(manager: UndoManager) { undoManager = manager }

    fun appendToStringBuilder(sb: StringBuilder) {
        sb.ensureCapacity(sb.length + length)
        lock(false)
        try {
            val linesCount = lineCount
            for (i in 0 until linesCount) {
                val line = lines[i]
                line.appendTo(sb)
                sb.append(line.lineSeparatorSafe.content)
            }
        } finally { unlock(false) }
    }

    val cursor: Cursor get() {
        if (_cursor == null) _cursor = Cursor(this)
        return _cursor!!
    }

    fun isCursorCreated() = _cursor != null

    private fun dispatchBeforeReplace() {
        undoManager.beforeReplace(this)
        _cursor?.beforeReplace()
        if (indexer is ContentListener) (indexer as ContentListener).beforeReplace(this)
        contentListeners.forEach { it.beforeReplace(this) }
    }

    private fun dispatchAfterDelete(a: Int, b: Int, c: Int, d: Int, e: CharSequence) {
        undoManager.afterDelete(this, a, b, c, d, e)
        _cursor?.afterDelete(a, b, c, d, e)
        if (indexer is ContentListener) (indexer as ContentListener).afterDelete(this, a, b, c, d, e)
        contentListeners.forEach { it.afterDelete(this, a, b, c, d, e) }
    }

    private fun dispatchBeforeModification() {
        undoManager.beforeModification(this)
        contentListeners.forEach { it.beforeModification(this) }
    }

    private fun dispatchAfterInsert(a: Int, b: Int, c: Int, d: Int, e: CharSequence) {
        undoManager.afterInsert(this, a, b, c, d, e)
        _cursor?.afterInsert(a, b, c, d, e)
        if (indexer is ContentListener) (indexer as ContentListener).afterInsert(this, a, b, c, d, e)
        contentListeners.forEach { it.afterInsert(this, a, b, c, d, e) }
    }

    internal fun checkIndex(index: Int, checkType: Int) {
        if ((if (checkType == CHECK_TYPE_READ) index >= length else index > length) || index < 0)
            throw StringIndexOutOfBoundsException("Index $index out of bounds. length:$length")
    }

    internal fun checkLine(line: Int) {
        if (line >= lineCount || line < 0) throw StringIndexOutOfBoundsException("Line $line out of bounds. line count:$lineCount")
    }

    internal fun checkLineAndColumn(line: Int, column: Int, checkType: Int) {
        checkLine(line)
        val text = lines[line]
        when (checkType) {
            CHECK_TYPE_READ -> {
                val len = text.length + text.lineSeparatorSafe.length
                if (column >= len || column < 0) throw StringIndexOutOfBoundsException("Column $column out of bounds for READ. line: $line, valid range: [0, $len)")
            }
            CHECK_TYPE_CURSOR -> {
                val len = text.length
                if (column > len || column < 0) throw StringIndexOutOfBoundsException("Column $column out of bounds for CURSOR. line: $line, valid range: [0, $len]")
            }
            CHECK_TYPE_INDEX -> {
                val len = text.length + text.lineSeparatorSafe.length
                if (line == lineCount - 1) {
                    if (column > len || column < 0) throw StringIndexOutOfBoundsException("Column $column out of bounds for INDEX. line: $line, valid range: [0, $len]")
                } else if (column >= len || column < 0) throw StringIndexOutOfBoundsException("Column $column out of bounds for INDEX. line: $line, valid range: [0, $len)")
            }
        }
    }

    fun copyText() = copyText(true)
    fun copyText(newContentThreadSafe: Boolean) = copyText(newContentThreadSafe, false)

    fun copyText(newContentThreadSafe: Boolean, shallow: Boolean): Content {
        lock(false)
        return try {
            val n = Content(null, newContentThreadSafe)
            n.lines.removeAt(0)
            if (n.lines is ArrayList<*>) (n.lines as ArrayList<ContentLine>).ensureCapacity(lineCount)
            if (shallow) {
                for (line in lines) line.retain()
                n.lines.addAll(lines)
            } else for (line in lines) n.lines.add(ContentLine(line))
            n.textLength = textLength
            n
        } finally { unlock(false) }
    }

    fun copyTextShallow() = copyTextShallow(false)
    fun copyTextShallow(newContentThreadSafe: Boolean) = copyText(newContentThreadSafe, true)

    fun release() {
        lock(true)
        try {
            lines.forEach { it.release() }
            lines.clear()
            textLength = 0
            _cursor = null
            bidi.destroy()
        } finally { unlock(true) }
    }

    fun runReadActionsOnLines(startLine: Int, endLine: Int, consumer: ContentLineConsumer) {
        lock(false)
        try {
            for (i in startLine..endLine) {
                val t = lines[i]
                consumer.accept(i, t, bidi.getLineDirections(t, i))
            }
        } finally { unlock(false) }
    }

    fun runReadActionsOnLines(startLine: Int, endLine: Int, consumer: ContentLineConsumer2) {
        lock(false)
        try {
            val flag = ContentLineConsumer2.AbortFlag()
            for (i in startLine..endLine) {
                if (flag.set) break
                consumer.accept(i, lines[i], flag)
            }
        } finally { unlock(false) }
    }

    fun interface ContentLineConsumer {
        fun accept(lineIndex: Int, line: ContentLine, dirs: Directions)
    }

    fun interface ContentLineConsumer2 {
        fun accept(lineIndex: Int, line: ContentLine, flag: AbortFlag)
        class AbortFlag { @JvmField var set = false }
    }

    companion object {
        const val CHECK_TYPE_CURSOR = 0
        const val CHECK_TYPE_READ = 1
        const val CHECK_TYPE_INDEX = 2
        private const val DEFAULT_LIST_CAPACITY = 1000
        private const val DEFAULT_MAX_UNDO_STACK_SIZE = 500

        @JvmStatic var initialLineCapacity = DEFAULT_LIST_CAPACITY
            set(capacity) {
                if (capacity <= 0) throw IllegalArgumentException("capacity > 0 required")
                field = capacity
            }

        private fun textEquals(a: ContentLine, b: ContentLine): Boolean {
            if (a.length != b.length) return false
            if (a === b) return true
            for (i in 0 until a.length) if (a[i] != b[i]) return false
            return true
        }
    }
}
