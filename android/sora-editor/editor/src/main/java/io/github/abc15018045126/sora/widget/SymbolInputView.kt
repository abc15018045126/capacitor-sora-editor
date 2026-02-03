package io.github.abc15018045126.sora.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.NonNull
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.widget.snippet.SnippetController


class SymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    var textColor: Int = 0
        set(value) {
            field = value
            for (i in 0 until childCount) {
                (getChildAt(i) as? Button)?.setTextColor(value)
            }
        }

    private var editor: CodeEditor? = null

    init {
        setBackgroundColor(context.resources.getColor(R.color.defaultSymbolInputBackgroundColor))
        orientation = HORIZONTAL
        textColor = context.resources.getColor(R.color.defaultSymbolInputTextColor)
    }


    fun bindEditor(editor: CodeEditor?) {
        this.editor = editor
    }


    fun removeSymbols() {
        removeAllViews()
    }


    fun addSymbols(@NonNull display: Array<String>, @NonNull insertText: Array<String>) {
        val count = Math.max(display.size, insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall)
            btn.text = display[i]
            btn.background = ColorDrawable(Color.TRANSPARENT)
            btn.setTextColor(textColor)
            addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            val finalI = i
            btn.setOnClickListener {
                val currentEditor = editor
                if (currentEditor == null || !currentEditor.isEditable) {
                    return@setOnClickListener
                }

                if ("\t" == insertText[finalI]) {
                    val snippetController: io.github.abc15018045126.sora.widget.snippet.SnippetController? = currentEditor.snippetController
                    if (snippetController != null && snippetController.isInSnippet()) {
                        snippetController.shiftToNextTabStop()
                    } else {
                        currentEditor.indentOrCommitTab()
                    }
                } else {
                    currentEditor.insertText(insertText[finalI], 1)
                }
            }
        }
    }

    fun forEachButton(@NonNull consumer: ButtonConsumer) {
        for (i in 0 until childCount) {
            (getChildAt(i) as? Button)?.let { consumer.accept(it) }
        }
    }

    fun interface ButtonConsumer {
        fun accept(@NonNull btn: Button)
    }
}
