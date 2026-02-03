package io.github.abc15018045126.sora.lang.completion

import android.graphics.drawable.Drawable
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.widget.CodeEditor


abstract class CompletionItem {


    @JvmField
    var icon: Drawable? = null


    @JvmField
    var label: CharSequence? = null


    @JvmField
    var desc: CharSequence? = null


    @JvmField
    var kind: CompletionItemKind? = null


    @JvmField
    var prefixLength: Int = 0


    @JvmField
    var sortText: String? = null


    @JvmField
    var filterText: String? = null

    @JvmField
    var extra: Any? = null

    constructor(label: CharSequence) : this(label, null)

    constructor(label: CharSequence, desc: CharSequence?) : this(label, desc, null)

    constructor(label: CharSequence, desc: CharSequence?, icon: Drawable?) {
        this.label = label
        this.desc = desc
        this.icon = icon
    }

    open fun label(label: CharSequence): CompletionItem {
        this.label = label
        return this
    }

    open fun desc(desc: CharSequence): CompletionItem {
        this.desc = desc
        return this
    }

    open fun kind(kind: CompletionItemKind): CompletionItem {
        this.kind = kind
        return this
    }

    open fun icon(icon: Drawable): CompletionItem {
        this.icon = icon
        return this
    }


    open fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }


    abstract fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int)
}
