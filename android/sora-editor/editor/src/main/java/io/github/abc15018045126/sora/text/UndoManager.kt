package io.github.abc15018045126.sora.text

import android.os.*
import java.util.*
import kotlin.math.abs

class UndoManager : ContentListener, Parcelable {
    private val actionStack: MutableList<ContentAction> = ArrayList()
    private var undoEnabled = false
    private var maxStackSize = 0
    private var insertAction: InsertAction? = null
    private var deleteAction: DeleteAction? = null
    private var targetContent: Content? = null
    private var replaceMark = false
    private var stackPointer = 0
    private var ignoreModification = false
    private var forceNewMultiAction = false
    private var memorizedCursorRange: TextRange? = null

    constructor()

    private constructor(parcel: Parcel) {
        maxStackSize = parcel.readInt()
        stackPointer = parcel.readInt()
        undoEnabled = parcel.readInt() > 0
        repeat(parcel.readInt()) {
            actionStack.add(parcel.readParcelable(UndoManager::class.java.classLoader)!!)
        }
    }

    override fun describeContents() = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(maxStackSize)
        parcel.writeInt(stackPointer)
        parcel.writeInt(if (undoEnabled) 1 else 0)
        parcel.writeInt(actionStack.size)
        for (a in actionStack) parcel.writeParcelable(a, flags)
    }

    val isModifyingContent get() = ignoreModification

    fun undo(content: Content): TextRange? {
        if (!canUndo() || isModifyingContent) return null
        ignoreModification = true
        val action = actionStack[--stackPointer]
        action.undo(content)
        ignoreModification = false
        return action.cursor
    }

    fun redo(content: Content) {
        if (!canRedo() || isModifyingContent) return
        ignoreModification = true
        actionStack[stackPointer++].redo(content)
        ignoreModification = false
    }

    internal fun onExitBatchEdit() {
        forceNewMultiAction = true
        if (actionStack.isNotEmpty() && actionStack.last() is MultiAction) {
            val a = actionStack.last() as MultiAction
            if (a.actions.size == 1) actionStack[actionStack.lastIndex] = a.actions[0]
        }
    }

    fun canUndo() = isUndoEnabled && stackPointer > 0
    fun canRedo() = isUndoEnabled && stackPointer < actionStack.size

    var isUndoEnabled: Boolean
        get() = undoEnabled
        set(v) { undoEnabled = v; if (!v) cleanStack() }

    var maxUndoStackSize: Int
        get() = maxStackSize
        set(v) { if (v <= 0) throw IllegalArgumentException(); maxStackSize = v; cleanStack() }

    private fun cleanStack() {
        if (!undoEnabled) { actionStack.clear(); stackPointer = 0 }
        else while (stackPointer > 1 && actionStack.size > maxStackSize) { actionStack.removeAt(0); stackPointer-- }
    }

    private fun cleanBeforePush() { while (stackPointer < actionStack.size) actionStack.removeAt(actionStack.lastIndex) }

    private fun pushAction(content: Content, action: ContentAction) {
        if (!isUndoEnabled) return
        cleanBeforePush()
        if (content.isInBatchEdit) {
            if (actionStack.isEmpty() || actionStack.last() !is MultiAction || forceNewMultiAction) {
                actionStack.add(MultiAction().apply { addAction(action); cursor = action.cursor })
                stackPointer++
            } else (actionStack.last() as MultiAction).addAction(action)
        } else {
            if (actionStack.isNotEmpty() && actionStack.last().canMerge(action)) actionStack.last().merge(action)
            else { actionStack.add(action); stackPointer++ }
        }
        forceNewMultiAction = false; cleanStack()
    }

    fun exitReplaceMode() {
        if (replaceMark && deleteAction != null) pushAction(targetContent!!, deleteAction!!)
        replaceMark = false; targetContent = null
    }

    override fun beforeReplace(content: Content) { if (!ignoreModification) { replaceMark = true; targetContent = content } }

    override fun afterInsert(content: Content, sl: Int, sc: Int, el: Int, ec: Int, text: CharSequence) {
        if (ignoreModification) return
        val ins = InsertAction().apply { startLine = sl; startColumn = sc; endLine = el; endColumn = ec; this.text = text }
        insertAction = ins
        if (replaceMark && deleteAction != null) pushAction(content, ReplaceAction().apply { delete = deleteAction; insert = ins; cursor = memorizedCursorRange })
        else { ins.cursor = memorizedCursorRange; pushAction(content, ins) }
        deleteAction = null; insertAction = null; replaceMark = false
    }

    override fun afterDelete(content: Content, sl: Int, sc: Int, el: Int, ec: Int, text: CharSequence) {
        if (ignoreModification) return
        val del = DeleteAction().apply { startLine = sl; startColumn = sc; endLine = el; endColumn = ec; this.text = text; cursor = memorizedCursorRange }
        deleteAction = del; if (!replaceMark) pushAction(content, del)
    }

    override fun beforeModification(content: Content) {
        if (undoEnabled && content.isCursorCreated() && !(replaceMark && deleteAction != null)) memorizedCursorRange = content.cursor.getRange()
    }

    abstract class ContentAction : Parcelable {
        @JvmField @Transient var cursor: TextRange? = null
        abstract fun undo(c: Content)
        abstract fun redo(c: Content)
        abstract fun canMerge(a: ContentAction): Boolean
        abstract fun merge(a: ContentAction)
    }

    class InsertAction : ContentAction {
        @JvmField var startLine = 0
        @JvmField var endLine = 0
        @JvmField var startColumn = 0
        @JvmField var endColumn = 0
        @JvmField @Transient var createTime = System.currentTimeMillis()
        @JvmField var text: CharSequence? = null

        constructor()
        private constructor(p: Parcel) {
            startLine = p.readInt(); startColumn = p.readInt(); endLine = p.readInt(); endColumn = p.readInt(); text = p.readString()
        }

        override fun undo(c: Content) { c.delete(startLine, startColumn, endLine, endColumn) }
        override fun redo(c: Content) { c.insert(startLine, startColumn, text!!) }
        override fun canMerge(a: ContentAction) = a is InsertAction && a.startColumn == endColumn && a.startLine == endLine && (a.text?.length ?: 0) + (text?.length ?: 0) < 10000 && abs(a.createTime - createTime) < sMergeTimeLimit
        override fun merge(a: ContentAction) {
            val ac = a as InsertAction; endColumn = ac.endColumn; endLine = ac.endLine
            val sb = if (text is StringBuilder) text as StringBuilder else StringBuilder(text!!).also { text = it }
            sb.append(ac.text)
        }

        override fun describeContents() = 0
        override fun writeToParcel(p: Parcel, f: Int) {
            p.writeInt(startLine); p.writeInt(startColumn); p.writeInt(endLine); p.writeInt(endColumn); p.writeString(text?.toString())
        }

        companion object CREATOR : Parcelable.Creator<InsertAction> {
            override fun createFromParcel(p: Parcel) = InsertAction(p)
            override fun newArray(s: Int) = arrayOfNulls<InsertAction>(s)
        }
    }

    class MultiAction : ContentAction {
        val actions = ArrayList<ContentAction>()
        constructor()
        private constructor(p: Parcel) { repeat(p.readInt()) { actions.add(p.readParcelable(MultiAction::class.java.classLoader)!!) } }

        fun addAction(a: ContentAction) {
            if (actions.isNotEmpty() && actions.last().canMerge(a)) actions.last().merge(a) else actions.add(a)
        }

        override fun undo(c: Content) { for (i in actions.lastIndex downTo 0) actions[i].undo(c) }
        override fun redo(c: Content) { for (a in actions) a.redo(c) }
        override fun canMerge(a: ContentAction) = false
        override fun merge(a: ContentAction) = throw UnsupportedOperationException()
        override fun describeContents() = 0
        override fun writeToParcel(p: Parcel, f: Int) {
            p.writeInt(actions.size); for (a in actions) p.writeParcelable(a, f)
        }

        companion object CREATOR : Parcelable.Creator<MultiAction> {
            override fun createFromParcel(p: Parcel) = MultiAction(p)
            override fun newArray(s: Int) = arrayOfNulls<MultiAction>(s)
        }
    }

    class DeleteAction : ContentAction {
        @JvmField var startLine = 0
        @JvmField var endLine = 0
        @JvmField var startColumn = 0
        @JvmField var endColumn = 0
        @JvmField @Transient var createTime = System.currentTimeMillis()
        @JvmField var text: CharSequence? = null

        constructor()
        private constructor(p: Parcel) {
            startLine = p.readInt(); startColumn = p.readInt(); endLine = p.readInt(); endColumn = p.readInt(); text = p.readString()
        }

        override fun undo(c: Content) { c.insert(startLine, startColumn, text!!) }
        override fun redo(c: Content) { c.delete(startLine, startColumn, endLine, endColumn) }
        override fun canMerge(a: ContentAction) = a is DeleteAction && a.endColumn == startColumn && a.endLine == startLine && (a.text?.length ?: 0) + (text?.length ?: 0) < 10000 && abs(a.createTime - createTime) < sMergeTimeLimit
        override fun merge(a: ContentAction) {
            val ac = a as DeleteAction; startColumn = ac.startColumn; startLine = ac.startLine
            val sb = if (text is StringBuilder) text as StringBuilder else StringBuilder(text!!).also { text = it }
            sb.insert(0, ac.text)
        }

        override fun describeContents() = 0
        override fun writeToParcel(p: Parcel, f: Int) {
            p.writeInt(startLine); p.writeInt(startColumn); p.writeInt(endLine); p.writeInt(endColumn); p.writeString(text?.toString())
        }

        companion object CREATOR : Parcelable.Creator<DeleteAction> {
            override fun createFromParcel(p: Parcel) = DeleteAction(p)
            override fun newArray(s: Int) = arrayOfNulls<DeleteAction>(s)
        }
    }

    class ReplaceAction : ContentAction {
        @JvmField var insert: InsertAction? = null
        @JvmField var delete: DeleteAction? = null

        constructor()
        private constructor(p: Parcel) {
            insert = p.readParcelable(ReplaceAction::class.java.classLoader)
            delete = p.readParcelable(ReplaceAction::class.java.classLoader)
        }

        override fun undo(c: Content) { insert?.undo(c); delete?.undo(c) }
        override fun redo(c: Content) { delete?.redo(c); insert?.redo(c) }
        override fun canMerge(a: ContentAction) = false
        override fun merge(a: ContentAction) = throw UnsupportedOperationException()
        override fun describeContents() = 0
        override fun writeToParcel(p: Parcel, f: Int) { p.writeParcelable(insert, f); p.writeParcelable(delete, f) }

        companion object CREATOR : Parcelable.Creator<ReplaceAction> {
            override fun createFromParcel(p: Parcel) = ReplaceAction(p)
            override fun newArray(s: Int) = arrayOfNulls<ReplaceAction>(s)
        }
    }

    companion object {
        @JvmStatic var sMergeTimeLimit = 8000L
        @JvmField val CREATOR = object : Parcelable.Creator<UndoManager> {
            override fun createFromParcel(p: Parcel) = UndoManager(p)
            override fun newArray(s: Int) = arrayOfNulls<UndoManager>(s)
        }
    }
}
