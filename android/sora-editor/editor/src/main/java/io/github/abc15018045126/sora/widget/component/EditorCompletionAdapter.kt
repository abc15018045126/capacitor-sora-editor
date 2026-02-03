package io.github.abc15018045126.sora.widget.component

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import io.github.abc15018045126.sora.lang.completion.CompletionItem
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme


abstract class EditorCompletionAdapter : BaseAdapter() {

    protected var window: EditorAutoCompletion? = null
    var items: List<CompletionItem>? = null


    fun attachValues(window: EditorAutoCompletion, items: List<CompletionItem>) {
        this.window = window
        this.items = items
    }

    override fun getItem(position: Int): CompletionItem {
        return items!![position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun getCount(): Int {
        return items?.size ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent, position == window?.currentPosition)
    }


    protected fun getColorScheme(): EditorColorScheme {
        return window!!.editor.colorScheme
    }


    protected fun getThemeColor(type: Int): Int {
        return getColorScheme().getColor(type)
    }


    protected fun getContext(): Context {
        return window!!.editor.context
    }


    abstract fun getItemHeight(): Int


    protected abstract fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View

}
