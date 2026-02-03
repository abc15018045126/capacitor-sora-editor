package io.github.abc15018045126.sora.text

import android.annotation.SuppressLint
import android.os.Build
import android.text.DynamicLayout
import android.text.Editable
import android.text.Layout
import android.text.Selection
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

class TextLayoutHelper private constructor() {
    private val text = Editable.Factory.getInstance().newEditable("")
    private val layout: DynamicLayout

    init {
        val paint = TextPaint()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            layout = DynamicLayout(text, paint, Int.MAX_VALUE / 2, Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true)
            try {
                @SuppressLint("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
                Layout::class.java.getDeclaredField("mTextDir").apply {
                    isAccessible = true
                    set(layout, TextDirectionHeuristics.FIRSTSTRONG_LTR)
                }
            } catch (e: Exception) {}
        } else {
            layout = DynamicLayout.Builder.obtain(text, paint, Int.MAX_VALUE / 2)
                .setIncludePad(true)
                .setLineSpacing(0f, 0f)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
        }
    }

    fun getCurPosLeft(offset: Int, s: CharSequence): Int = calculatePos(offset, s) { Selection.moveLeft(text, layout) }

    fun getCurPosRight(offset: Int, s: CharSequence): Int = calculatePos(offset, s) { Selection.moveRight(text, layout) }

    private inline fun calculatePos(offset: Int, s: CharSequence, moveAction: () -> Unit): Int {
        val left = max(0, offset - CHAR_FACTOR)
        text.append(s, left, min(s.length, offset + CHAR_FACTOR + 1))
        Selection.setSelection(text, min(offset - left, text.length))
        try {
            moveAction()
            return left + Selection.getSelectionStart(text)
        } finally {
            text.clear()
            Selection.removeSelection(text)
        }
    }

    companion object {
        private val sLocal = ThreadLocal<TextLayoutHelper>()
        private const val CHAR_FACTOR = 64

        @JvmStatic
        fun get(): TextLayoutHelper = sLocal.get() ?: TextLayoutHelper().also { sLocal.set(it) }
    }
}
