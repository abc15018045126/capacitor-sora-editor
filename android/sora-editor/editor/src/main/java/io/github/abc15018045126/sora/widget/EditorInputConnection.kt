package io.github.abc15018045126.sora.widget

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import android.view.inputmethod.TextSnapshot
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.event.ContentChangeEvent
import io.github.abc15018045126.sora.event.ImePrivateCommandEvent
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.event.EventReceiver
import io.github.abc15018045126.sora.event.Unsubscribe
import io.github.abc15018045126.sora.text.ComposingText
import io.github.abc15018045126.sora.util.Logger
import kotlin.math.max
import kotlin.math.min

internal class EditorInputConnection(private val editor: CodeEditor) : BaseInputConnection(editor, true) {
    @JvmField internal var composingText = ComposingText()
    internal var imeConsumingInput = false
    private var connectionInvalid = false

    init {
        editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
            when (event.action) {
                ContentChangeEvent.ACTION_INSERT -> composingText.shiftOnInsert(event.changeStart.index, event.changeEnd.index)
                ContentChangeEvent.ACTION_DELETE -> composingText.shiftOnDelete(event.changeStart.index, event.changeEnd.index)
            }
        }
    }

    internal fun markInvalid() {
        connectionInvalid = true
        composingText.reset()
        resetBatchEdit()
        editor.invalidate()
    }

    internal fun reset() {
        resetBatchEdit()
        composingText.reset()
        connectionInvalid = false
        imeConsumingInput = false
    }

    private fun resetBatchEdit() {
        while (editor.text.isInBatchEdit) editor.text.endBatchEdit()
    }

    private val cursor get() = editor.cursor!!

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!editor.isEditable || connectionInvalid || text == null) return false
        if ("\n" == text.toString()) sendKeyClick(KeyEvent.KEYCODE_ENTER)
        else commitTextInternal(text, true)
        return true
    }

    @Synchronized override fun closeConnection() {
        super.closeConnection()
        resetBatchEdit()
        composingText.reset()
        editor.onCloseConnection()
    }

    override fun getCursorCapsMode(reqModes: Int) = TextUtils.getCapsMode(editor.text, cursor.left, reqModes)

    private fun getTextRegionInternal(start: Int, end: Int, flags: Int, ignoreIPCLimit: Boolean): CharSequence? {
        val s = max(0, min(start, end))
        val limit = editor.text.length
        val e = if (ignoreIPCLimit) min(max(start, end), limit) else min(max(start, end), min(s + max(0, editor.props!!.maxIPCTextLength), limit))
        val sub = editor.text.subSequence(s, e).toString()
        if (flags == GET_TEXT_WITH_STYLES) {
            val text = SpannableStringBuilder(sub)
            if (composingText.isComposing()) {
                try {
                    val ts = max(0, composingText.startIndex - s)
                    val te = min(text.length, composingText.endIndex - s)
                    if (ts < text.length && te > 0) text.setSpan(Spanned.SPAN_COMPOSING, ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } catch (_: IndexOutOfBoundsException) { }
            }
            return text
        }
        return sub
    }

    internal fun getTextRegion(start: Int, end: Int, flags: Int) = try {
        getTextRegionInternal(start, end, flags, false)
    } catch (e: IndexOutOfBoundsException) {
        logger.w("Failed to get text region for IME", e)
        ""
    }

    protected fun getTextRegionUnlimited(start: Int, end: Int, flags: Int) = try {
        getTextRegionInternal(start, end, flags, true)
    } catch (e: IndexOutOfBoundsException) {
        logger.w("Failed to get text region for IME", e)
        ""
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) return null
        val left = cursor.left
        val right = cursor.right
        return if (left == right) null else getTextRegion(left, right, flags)
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) return ""
        val end = cursor.left
        val start = max(end - length, end - editor.props!!.maxIPCTextLength)
        return getTextRegion(start, end, flags)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) return ""
        val end = cursor.right
        return getTextRegion(end, end + length, flags)
    }

    private fun sendKeyClick(keyCode: Int) {
        val time = SystemClock.uptimeMillis()
        sendKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE))
        sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), time, KeyEvent.ACTION_UP, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE))
    }

    internal fun commitTextInternal(text: CharSequence, applyAutoIndent: Boolean) {
        var mutableText = text
        val composingStateBefore = composingText.isComposing()
        if (editor.props!!.trackComposingTextOnCommit && composingText.isComposing()) {
            val composingStr = editor.text.subSequence(composingText.startIndex, composingText.endIndex).toString()
            val commitText = mutableText.toString()
            if (composingText.endIndex == cursor.left && !cursor.isSelected() && commitText.startsWith(composingStr) && commitText.length > composingStr.length) {
                mutableText = commitText.substring(composingStr.length)
                composingText.reset()
            } else deleteComposingText()
        } else if (composingText.isComposing()) deleteComposingText()

        editor.commitText(mutableText, applyAutoIndent)
        if (composingStateBefore) endBatchEdit()
    }

    private fun deleteComposingText() {
        if (!composingText.isComposing()) return
        try {
            editor.text.delete(composingText.startIndex, composingText.endIndex)
        } catch (e: IndexOutOfBoundsException) { e.printStackTrace() }
        composingText.reset()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (!editor.isEditable || connectionInvalid || beforeLength < 0 || afterLength < 0) return false
        if (beforeLength == 1 && afterLength == 0 && !composingText.isComposing()) {
            editor.deleteText()
            return true
        }
        if (beforeLength > 0 && afterLength > 0) beginBatchEdit()
        val composing = composingText.isComposing()
        var cStart = if (composing) composingText.startIndex else 0
        var cEnd = if (composing) composingText.endIndex else 0
        var rEnd = cursor.left
        var rStart = max(0, rEnd - beforeLength)
        editor.text.delete(rStart, rEnd)
        if (composing) {
            val delta = max(0, max(rStart, cStart) - rStart)
            cEnd -= max(0, min(rEnd, cEnd) - max(rStart, cStart)) + delta
            cStart -= delta
        }
        rStart = cursor.right
        rEnd = min(rStart + afterLength, editor.text.length)
        editor.text.delete(rStart, rEnd)
        if (composing) {
            val delta = max(0, max(rStart, cStart) - rStart)
            cEnd -= max(0, min(rEnd, cEnd) - max(rStart, cStart)) + delta
            cStart -= delta
        }
        if (beforeLength > 0 && afterLength > 0) endBatchEdit()
        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int) = false

    @Synchronized override fun beginBatchEdit() = if (editor.props!!.disallowSuggestions) editor.text.isInBatchEdit else editor.text.beginBatchEdit()

    @Synchronized override fun endBatchEdit(): Boolean {
        val inBatch = editor.text.endBatchEdit()
        if (!inBatch) editor.updateSelection()
        return inBatch
    }

    private fun deleteSelected() { if (cursor.isSelected()) editor.deleteText() }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText()) return false
        if (editor.props!!.disallowSuggestions) {
            composingText.reset()
            commitText(text, 0)
            return false
        }
        if (TextUtils.indexOf(text, '\n') != -1) return false
        if (!composingText.isComposing()) {
            composingText.preSetComposing = true
            deleteSelected()
            beginBatchEdit()
            editor.commitText(text)
            composingText.preSetComposing = false
            composingText.set(cursor.left - text.length, cursor.left)
            editor.updateCursor()
        } else {
            if (editor.props!!.minimizeComposingTextUpdate) setComposingTextCompat(text.toString())
            else editor.text.replace(composingText.startIndex, composingText.endIndex, text)
            composingText.adjustLength(text.length)
        }
        if (text.isEmpty()) {
            finishComposingText()
            return true
        }
        return true
    }

    private fun setComposingTextCompat(text: String) {
        val content = editor.text
        val current = content.substring(composingText.startIndex, composingText.endIndex)
        if (current == text) return
        if (current.length < text.length && text.startsWith(current)) {
            val pos = content.indexer.getCharPosition(composingText.endIndex)
            content.insert(pos.line, pos.column, text.substring(current.length))
        } else if (current.length > text.length && current.startsWith(text)) {
            content.delete(composingText.endIndex - (current.length - text.length), composingText.endIndex)
        } else content.replace(composingText.startIndex, composingText.endIndex, text)
    }

    override fun finishComposingText(): Boolean {
        if (!editor.isEditable || connectionInvalid || editor.props!!.disallowSuggestions) return false
        composingText.reset()
        endBatchEdit()
        editor.updateCursor()
        editor.invalidate()
        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (!editor.isEditable || connectionInvalid || editor.props!!.disallowSuggestions) return false
        val s = max(0, min(start, end)) // getWrappedIndex logic inlined + swap logic
        val e = min(max(start, end), editor.text.length)
        if (s == cursor.left && e == cursor.right) return true
        val startPos = editor.text.indexer.getCharPosition(s)
        val endPos = editor.text.indexer.getCharPosition(e)
        editor.setSelectionRegion(startPos.line, startPos.column, endPos.line, endPos.column, false, SelectionChangeEvent.CAUSE_IME)
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText() || editor.props!!.disallowSuggestions) return false
        if (start == end) {
            finishComposingText()
            return true
        }
        try {
            val s = max(0, min(start, end))
            val e = min(max(start, end), editor.text.length)
            if (s >= e) return false
            composingText.set(s, e)
            editor.invalidate()
        } catch (e: IndexOutOfBoundsException) {
            logger.w("set composing region for IME failed", e)
            return false
        }
        beginBatchEdit()
        return true
    }

    override fun performContextMenuAction(id: Int): Boolean {
        when (id) {
            android.R.id.selectAll -> editor.selectAll()
            android.R.id.cut -> {
                editor.copyText()
                if (cursor.isSelected()) editor.deleteText()
            }
            android.R.id.paste, android.R.id.pasteAsPlainText -> editor.pasteText()
            android.R.id.copy -> editor.copyText()
            android.R.id.undo -> editor.undo()
            android.R.id.redo -> editor.redo()
            else -> return false
        }
        return true
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        editor.updateCursorAnchor()
        return true
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (editor.props!!.disallowSuggestions || editor.props!!.disableTextExtracting) return null
        editor.setExtracting(if ((flags and GET_EXTRACTED_TEXT_MONITOR) != 0) request else null)
        return editor.extractText(request!!)
    }

    override fun clearMetaKeyStates(states: Int) = editor.getKeyMetaStates().clearMetaStates(states).let { true }
    override fun reportFullscreenMode(enabled: Boolean) = false
    override fun getHandler() = editor.handler

    @RequiresApi(31) override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        if (editor.props!!.disallowSuggestions) return SurroundingText("", 0, 0, -1)
        require(!((beforeLength or afterLength) < 0)) { "length < 0" }
        var startOffset = max(0, cursor.left - beforeLength)
        startOffset = min(startOffset, cursor.left)
        val text = getTextRegionUnlimited(startOffset, min(editor.text.length, cursor.right + afterLength), flags)
        return SurroundingText(text ?: "", cursor.left - startOffset, cursor.right - startOffset, startOffset)
    }

    override fun setImeConsumesInput(imeConsumesInput: Boolean): Boolean {
        if (connectionInvalid) return false
        this.imeConsumingInput = imeConsumesInput
        editor.invalidate()
        return true
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (connectionInvalid) return false
        editor.dispatchEvent(ImePrivateCommandEvent(editor, action ?: "", data))
        return true
    }

    override fun replaceText(start: Int, end: Int, text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean {
        val length = editor.text.length
        if (start < 0 || end < 0 || start > end || start > length || end > length) return false
        beginBatchEdit()
        finishComposingText()
        setSelection(start, end)
        commitText(text, newCursorPosition)
        endBatchEdit()
        return true
    }

    @RequiresApi(33) override fun takeSnapshot(): TextSnapshot? {
        var composingStart = -1
        var composingEnd = -1
        if (composingText.isComposing()) {
            composingStart = composingText.startIndex
            composingEnd = composingText.endIndex
        }
        val surroundingText = getSurroundingText(MEMORY_EFFICIENT_TEXT_LENGTH / 2, MEMORY_EFFICIENT_TEXT_LENGTH / 2, GET_TEXT_WITH_STYLES) ?: return null
        val cursorCapsMode = getCursorCapsMode(TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES)
        return TextSnapshot(surroundingText, composingStart, composingEnd, cursorCapsMode)
    }

    companion object {
        private val logger = Logger.instance("EditorInputConnection")
        private const val MEMORY_EFFICIENT_TEXT_LENGTH = 2048
    }
}
