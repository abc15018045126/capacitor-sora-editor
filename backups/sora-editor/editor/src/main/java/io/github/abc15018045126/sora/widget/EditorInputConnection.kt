
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
import io.github.abc15018045126.sora.text.ComposingText
import io.github.abc15018045126.sora.text.Cursor
import io.github.abc15018045126.sora.util.Logger
import kotlin.math.max
import kotlin.math.min

/**
 * Connection between input method and editor
 *
 * @author abc15018045126
 */
internal class EditorInputConnection(private val editor: CodeEditor) : BaseInputConnection(editor, true) {

    @JvmField
    internal var composingText: ComposingText = ComposingText()

    @JvmField
    protected var imeConsumingInput: Boolean = false
    private var connectionInvalid: Boolean = false

    companion object {
        private val logger = Logger.instance("EditorInputConnection")
        private const val MEMORY_EFFICIENT_TEXT_LENGTH = 2048

        @JvmField
        var DEBUG = false
    }

    init {
        editor.subscribeEvent<ContentChangeEvent> { event, _ ->
            if (event.action == ContentChangeEvent.ACTION_INSERT) {
                composingText.shiftOnInsert(event.changeStart.index, event.changeEnd.index)
            } else if (event.action == ContentChangeEvent.ACTION_DELETE) {
                composingText.shiftOnDelete(event.changeStart.index, event.changeEnd.index)
            }
        }
    }

    fun markInvalid() {
        connectionInvalid = true
        composingText.reset()
        resetBatchEdit()
        editor.invalidate()
    }

    /**
     * Reset the state of this connection
     */
    fun reset() {
        resetBatchEdit()
        composingText.reset()
        connectionInvalid = false
        imeConsumingInput = false
    }

    private fun resetBatchEdit() {
        val content = editor.text
        while (content.isInBatchEdit) {
            content.endBatchEdit()
        }
    }

    /**
     * Private use.
     * Get the Cursor of Content displaying by Editor
     *
     * @return Cursor
     */
    private fun getCursor(): Cursor {
        return editor.cursor
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) logger.d("commitText text = $text, pos = $newCursorPosition")

        if (!editor.isEditable || connectionInvalid || text == null) {
            return false
        }

        if ("\n" == text.toString()) {
            // #67
            sendKeyClick(KeyEvent.KEYCODE_ENTER)
        } else {
            commitTextInternal(text, true)
        }
        return true
    }

    override fun closeConnection() {
        super.closeConnection()
        resetBatchEdit()
        composingText.reset()
        editor.onCloseConnection()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return TextUtils.getCapsMode(editor.text, getCursor().left, reqModes)
    }

    /**
     * Get content region internally
     */
    private fun getTextRegionInternal(start: Int, end: Int, flags: Int, ignoreIPCLimit: Boolean): CharSequence {
        val origin = editor.text
        var s = start
        var e = end
        if (s > e) {
            val tmp = s
            s = e
            e = tmp
        }
        if (s < 0) {
            s = 0
        }
        if (e > origin.length) {
            e = origin.length
        }
        if (e < s) {
            s = 0
            e = 0
        }
        if (!ignoreIPCLimit && e - s > editor.props.maxIPCTextLength) {
            e = s + max(0, editor.props.maxIPCTextLength)
        }
        val sub = origin.subSequence(s, e).toString()
        if (flags == GET_TEXT_WITH_STYLES) {
            val text = SpannableStringBuilder(sub)
            // Apply composing span
            if (composingText.isComposing) {
                try {
                    val originalComposingStart = composingText.startIndex
                    val originalComposingEnd = composingText.endIndex
                    var transferredStart = originalComposingStart - s
                    if (transferredStart >= text.length) {
                        return text
                    }
                    if (transferredStart < 0) {
                        transferredStart = 0
                    }
                    val transferredEnd = originalComposingEnd - s
                    if (transferredEnd <= 0) {
                        return text
                    }
                    val finalTransferredEnd = if (transferredEnd >= text.length) text.length else transferredEnd
                    text.setSpan(
                        Spanned.SPAN_COMPOSING,
                        transferredStart,
                        finalTransferredEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (ex: IndexOutOfBoundsException) {
                    //ignored
                }
            }
            return text
        }
        return sub
    }

    fun getTextRegion(start: Int, end: Int, flags: Int): CharSequence {
        try {
            val res = getTextRegionInternal(start, end, flags, false)
            if (DEBUG) logger.d("getTextRegion result:$res")
            return res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            return ""
        }
    }

    fun getTextRegionUnlimited(start: Int, end: Int, flags: Int): CharSequence {
        try {
            val res = getTextRegionInternal(start, end, flags, true)
            if (DEBUG) logger.d("getTextRegion result:$res")
            return res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            return ""
        }
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        if (editor.props.disallowSuggestions) {
            return null
        }
        //This text should be limited because when the user try to select all text
        //it can be quite large text and costs time, which will finally cause ANR
        val left = getCursor().left
        val right = getCursor().right
        return if (left == right) null else getTextRegion(left, right, flags)
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence {
        if (editor.props.disallowSuggestions) {
            return ""
        }
        val end = getCursor().left
        val start = max(end - length, end - editor.props.maxIPCTextLength)
        return getTextRegion(start, end, flags)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence {
        if (editor.props.disallowSuggestions) {
            return ""
        }
        val end = getCursor().right
        return getTextRegion(end, end + length, flags)
    }

    private fun sendKeyClick(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        sendKeyEvent(
            KeyEvent(
                SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    fun commitTextInternal(text: CharSequence, applyAutoIndent: Boolean) {
        val composingStateBefore = composingText.isComposing
        var finalText = text
        // NOTE: Text styles are ignored by editor
        // Remove composing text first if there is
        if (editor.props.trackComposingTextOnCommit) {
            if (composingText.isComposing) {
                val composingTextStr =
                    editor.text.subSequence(composingText.startIndex, composingText.endIndex).toString()
                val commitTextStr = text.toString()
                if (composingText.endIndex == getCursor().left && !getCursor().isSelected && commitTextStr.startsWith(
                        composingTextStr
                    ) && commitTextStr.length > composingTextStr.length
                ) {
                    finalText = commitTextStr.substring(composingTextStr.length)
                    composingText.reset()
                } else {
                    deleteComposingText()
                }
            }
        } else if (composingText.isComposing) {
            deleteComposingText()
        }

        editor.commitText(finalText, applyAutoIndent)

        if (composingStateBefore) {
            endBatchEdit()
        }
    }

    /**
     * Delete composing region
     */
    private fun deleteComposingText() {
        if (!composingText.isComposing) {
            return
        }
        try {
            editor.text.delete(composingText.startIndex, composingText.endIndex)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        composingText.reset()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (DEBUG) logger.d("deleteSurroundingText, before = $beforeLength, after = $afterLength")
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (beforeLength < 0 || afterLength < 0) {
            return false
        }

        // #170 Gboard compatible
        if (beforeLength == 1 && afterLength == 0 && !composingText.isComposing) {
            editor.deleteText()
            return true
        }

        // Start a batch edit when the operation can not be finished by one call to delete()
        if (beforeLength > 0 && afterLength > 0) {
            beginBatchEdit()
        }

        val composing = composingText.isComposing
        var composingStart = if (composing) composingText.startIndex else 0
        var composingEnd = if (composing) composingText.endIndex else 0

        val rangeEndBefore = getCursor().left
        var rangeStartBefore = max(0, rangeEndBefore - beforeLength)
        editor.text.delete(rangeStartBefore, rangeEndBefore)

        if (composing) {
            val crossStart = max(rangeStartBefore, composingStart)
            val crossEnd = min(rangeEndBefore, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStartBefore)
            composingEnd -= delta
            composingStart -= delta
        }

        val rangeStartAfter = getCursor().right
        var rangeEndAfter = min(editor.text.length, rangeStartAfter + afterLength)
        editor.text.delete(rangeStartAfter, rangeEndAfter)

        if (composing) {
            val crossStart = max(rangeStartAfter, composingStart)
            val crossEnd = min(rangeEndAfter, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStartAfter)
            composingEnd -= delta
            composingStart -= delta
        }

        if (beforeLength > 0 && afterLength > 0) {
            endBatchEdit()
        }

        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        // Unsupported operation
        // According to document, we should return false
        return false
    }

    override fun beginBatchEdit(): Boolean {
        if (DEBUG) logger.d("beginBatchEdit")
        if (editor.props.disallowSuggestions) {
            return editor.text.isInBatchEdit // Do not start new batch edit layer
        }
        return editor.text.beginBatchEdit()
    }

    override fun endBatchEdit(): Boolean {
        if (DEBUG) logger.d("endBatchEdit")
        val inBatch = editor.text.endBatchEdit()
        if (!inBatch) {
            editor.updateSelection()
        }
        return inBatch
    }

    private fun deleteSelected() {
        if (getCursor().isSelected) {
            // Delete selected text
            editor.deleteText()
        }
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) logger.d("setComposingText, text = $text, pos = $newCursorPosition")
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText()) {
            return false
        }
        if (editor.props.disallowSuggestions) {
            composingText.reset()
            commitText(text ?: "", 0)
            return false
        }
        if (text != null && TextUtils.indexOf(text, '\n') != -1) {
            return false
        }
        if (!composingText.isComposing) {
            // Create composing info
            composingText.preSetComposing = true
            deleteSelected()
            beginBatchEdit()
            editor.commitText(text ?: "")
            composingText.preSetComposing = false
            val textLen = text?.length ?: 0
            composingText.set(getCursor().left - textLen, getCursor().left)
            editor.updateCursor()
        } else {
            // Already have composing text
            if (composingText.isComposing) {
                if (editor.props.minimizeComposingTextUpdate) {
                    setComposingTextCompat(text.toString())
                } else {
                    editor.text.replace(composingText.startIndex, composingText.endIndex, text ?: "")
                }
                // Reset range
                composingText.adjustLength(text?.length ?: 0)
            }
        }
        if (text?.isEmpty() == true) {
            finishComposingText()
            return true
        }
        return true
    }

    private fun setComposingTextCompat(text: String) {
        val content = editor.text
        val current = content.substring(composingText.startIndex, composingText.endIndex)
        if (current == text) {
            return
        }
        if (current.length < text.length && text.startsWith(current)) {
            val pos = content.indexer.getCharPosition(composingText.endIndex)
            content.insert(pos.line, pos.column, text.substring(current.length))
        } else if (current.length > text.length && current.startsWith(text)) {
            content.delete(composingText.endIndex - (current.length - text.length), composingText.endIndex)
        } else {
            content.replace(composingText.startIndex, composingText.endIndex, text)
        }
    }

    override fun finishComposingText(): Boolean {
        if (DEBUG) logger.d("finishComposingText")
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (editor.props.disallowSuggestions) {
            return false
        }
        composingText.reset()
        endBatchEdit()
        editor.updateCursor()
        editor.invalidate()
        return true
    }

    private fun getWrappedIndex(index: Int): Int {
        if (index < 0) {
            return 0
        }
        if (index > editor.text.length) {
            return editor.text.length
        }
        return index
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (DEBUG) logger.d("setSelection, s = $start, e = $end")
        if (!editor.isEditable || connectionInvalid || editor.props.disallowSuggestions) {
            return false
        }
        var s = getWrappedIndex(start)
        var e = getWrappedIndex(end)
        if (s > e) {
            val tmp = s
            s = e
            e = tmp
        }
        if (s == getCursor().left && e == getCursor().right) {
            return true
        }
        val content = editor.text
        val startPos = content.indexer.getCharPosition(s)
        val endPos = content.indexer.getCharPosition(e)
        editor.setSelectionRegion(
            startPos.line,
            startPos.column,
            endPos.line,
            endPos.column,
            false,
            SelectionChangeEvent.CAUSE_IME
        )
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (DEBUG) logger.d("setComposingRegion, s = $start, e = $end")
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText() || editor.props.disallowSuggestions) {
            return false
        }
        if (start == end) {
            finishComposingText()
            return true
        }
        try {
            var s = start
            var e = end
            if (s > e) {
                val tmp = s
                s = e
                e = tmp
            }
            if (s < 0) {
                s = 0
            }
            val content = editor.text
            if (e > content.length) {
                e = content.length
            }
            if (s >= e) {
                return false
            }
            composingText.set(s, e)
            editor.invalidate()
        } catch (ex: IndexOutOfBoundsException) {
            logger.w("set composing region for IME failed", ex)
            return false
        }
        beginBatchEdit()
        return true
    }

    override fun performContextMenuAction(id: Int): Boolean {
        when (id) {
            android.R.id.selectAll -> {
                editor.selectAll()
                return true
            }
            android.R.id.cut -> {
                editor.copyText()
                if (getCursor().isSelected) {
                    editor.deleteText()
                }
                return true
            }
            android.R.id.paste,
            android.R.id.pasteAsPlainText -> {
                editor.pasteText()
                return true
            }
            android.R.id.copy -> {
                editor.copyText()
                return true
            }
            android.R.id.undo -> {
                editor.undo()
                return true
            }
            android.R.id.redo -> {
                editor.redo()
                return true
            }
        }
        return false
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        editor.updateCursorAnchor()
        return true
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (DEBUG) logger.d("getExtractedText, flags = $flags")
        if (editor.props.disallowSuggestions || editor.props.disableTextExtracting) {
            return null
        }
        if ((flags and GET_EXTRACTED_TEXT_MONITOR) != 0) {
            editor.setExtracting(request)
        } else {
            editor.setExtracting(null)
        }

        return if (request != null) editor.extractText(request) else null
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        editor.keyMetaStates.clearMetaStates(states)
        return true
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return false
    }

    override fun getHandler(): Handler? {
        return editor.handler
    }

    @RequiresApi(31)
    override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        if (DEBUG) logger.d("getSurroundingText, beforeLen = $beforeLength, afterLen = $afterLength")
        if (editor.props.disallowSuggestions) {
            return SurroundingText("", 0, 0, -1)
        }
        if ((beforeLength or afterLength) < 0) {
            throw IllegalArgumentException("length < 0")
        }
        val selStart = getCursor().left
        val startOffset = min(selStart, max(0, selStart - beforeLength))
        val text = getTextRegionUnlimited(
            startOffset,
            min(editor.text.length, getCursor().right + afterLength),
            flags
        )
        return SurroundingText(text, getCursor().left - startOffset, getCursor().right - startOffset, startOffset)
    }

    override fun setImeConsumesInput(imeConsumesInput: Boolean): Boolean {
        if (connectionInvalid) {
            return false
        }
        imeConsumingInput = imeConsumesInput
        editor.invalidate()
        return true
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (connectionInvalid) {
            return false
        }
        editor.dispatchEvent(ImePrivateCommandEvent(editor, action ?: "", data))
        return true
    }

    override fun replaceText(
        start: Int,
        end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?
    ): Boolean {
        if (DEBUG) {
            logger.d(
                "replaceText, st = " + start + ", ed = " + end + ", text = "
                        + text + ", nCurPos = " + newCursorPosition
            )
        }
        val length = editor.text.length
        if (start < 0 || end < 0 || start > end || start > length || end > length) {
            return false
        }
        beginBatchEdit()
        finishComposingText()
        setSelection(start, end)
        commitText(text ?: "", newCursorPosition)
        endBatchEdit()
        return true
    }

    @RequiresApi(33)
    override fun takeSnapshot(): TextSnapshot? {
        var composingStart = -1
        var composingEnd = -1
        if (composingText.isComposing) {
            composingStart = composingText.startIndex
            composingEnd = composingText.endIndex
        }

        val surroundingText = getSurroundingText(
            MEMORY_EFFICIENT_TEXT_LENGTH / 2,
            MEMORY_EFFICIENT_TEXT_LENGTH / 2, GET_TEXT_WITH_STYLES
        ) ?: return null

        val cursorCapsMode = getCursorCapsMode(
            TextUtils.CAP_MODE_CHARACTERS
                    or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES
        )

        return TextSnapshot(surroundingText, composingStart, composingEnd, cursorCapsMode)
    }
}
