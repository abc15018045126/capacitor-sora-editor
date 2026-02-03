package io.github.abc15018045126.sora.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.NonNull
import io.github.abc15018045126.sora.R

class SymbolInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    var textColor = 0
        set(value) {
            field = value
            for (i in 0 until childCount) (getChildAt(i) as? Button)?.setTextColor(value)
        }
    private var editor: CodeEditor? = null

    init {
        setBackgroundColor(context.resources.getColor(R.color.defaultSymbolInputBackgroundColor))
        orientation = HORIZONTAL
        textColor = context.resources.getColor(R.color.defaultSymbolInputTextColor)
    }

    fun bindEditor(editor: CodeEditor?) { this.editor = editor }
    fun removeSymbols() = removeAllViews()

    fun addSymbols(@NonNull display: Array<String>, @NonNull insertText: Array<String>) {
        val count = Math.max(display.size, insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall).apply {
                text = display[i]
                background = ColorDrawable(Color.TRANSPARENT)
                setTextColor(this@SymbolInputView.textColor)
                setOnClickListener {
                    val currentEditor = editor
                    if (currentEditor != null && currentEditor.isEditable) {
                        if ("\t" == insertText[i]) {
                            val snippetController = currentEditor.snippetController
                            if (snippetController != null && snippetController.isInSnippet()) snippetController.shiftToNextTabStop()
                            else currentEditor.indentOrCommitTab()
                        } else currentEditor.insertText(insertText[i], 1)
                    }
                }
            }
            addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    fun forEachButton(@NonNull consumer: ButtonConsumer) {
        for (i in 0 until childCount) (getChildAt(i) as? Button)?.let { consumer.accept(it) }
    }

    fun interface ButtonConsumer {
        fun accept(@NonNull btn: Button)
    }
}
