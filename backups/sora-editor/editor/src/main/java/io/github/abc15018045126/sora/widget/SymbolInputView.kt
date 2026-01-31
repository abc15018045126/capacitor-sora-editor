
package io.github.abc15018045126.sora.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import io.github.abc15018045126.sora.R

/**
 * A simple symbol input view implementation for editor.
 *
 * First, add your symbols by [addSymbols].
 * Then, bind a certain editor by [bindEditor] so that it works
 *
 * @author abc15018045126
 */
open class SymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var textColor: Int = 0
    private var editor: CodeEditor? = null

    init {
        init()
    }

    private fun init() {
        @Suppress("DEPRECATION")
        setBackgroundColor(context.resources.getColor(R.color.defaultSymbolInputBackgroundColor))
        orientation = HORIZONTAL
        @Suppress("DEPRECATION")
        setTextColor(context.resources.getColor(R.color.defaultSymbolInputTextColor))
    }

    /**
     * Bind editor for the view
     */
    fun bindEditor(editor: CodeEditor?) {
        this.editor = editor
    }

    /**
     * Get text color in the panel
     */
    fun getTextColor(): Int {
        return textColor
    }

    /**
     * Set text color in the panel
     */
    fun setTextColor(color: Int) {
        for (i in 0 until childCount) {
            (getChildAt(i) as Button).setTextColor(color)
        }
        textColor = color
    }

    /**
     * Remove all added symbols
     */
    fun removeSymbols() {
        removeAllViews()
    }

    /**
     * Add symbols to the view.
     *
     * @param display    The texts displayed in button
     * @param insertText The actual text to be inserted to editor when the button is clicked
     */
    fun addSymbols(display: Array<String>, insertText: Array<String>) {
        val count = maxOf(display.size, insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall)
            btn.text = display[i]
            btn.background = ColorDrawable(Color.TRANSPARENT)
            btn.setTextColor(textColor)
            addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            val insertion = insertText[i]
            btn.setOnClickListener {
                val ed = editor
                if (ed == null || !ed.isEditable) {
                    return@setOnClickListener
                }

                if ("\t" == insertion) {
                    if (ed.snippetController.isInSnippet()) {
                        ed.snippetController.shiftToNextTabStop()
                    } else {
                        ed.indentOrCommitTab()
                    }
                } else {
                    ed.insertText(insertion, 1)
                }
            }
        }
    }

    fun forEachButton(consumer: ButtonConsumer) {
        for (i in 0 until childCount) {
            consumer.accept(getChildAt(i) as Button)
        }
    }

    fun interface ButtonConsumer {
        fun accept(btn: Button)
    }

}
