

package io.github.abc15018045126.sora.widget.rendering

import android.os.Build
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.widget.CodeEditor


class RenderContext(val editor: CodeEditor) {

    val cache = RenderCache()

    val renderNodeHolder: RenderNodeHolder? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        RenderNodeHolder(editor)
    } else {
        null
    }

    val tabWidth
        get() = editor.tabWidth

    fun updateForRange(range: StyleUpdateRange) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            renderNodeHolder?.invalidateInRegion(range)
        }
    }

    fun invalidateRenderNodes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            renderNodeHolder?.invalidate()
        }
    }

    fun updateForInsertion(startLine: Int, endLine: Int) {
        cache.updateForInsertion(startLine, endLine)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            renderNodeHolder?.afterInsert(startLine, endLine)
        }
    }

    fun updateForDeletion(startLine: Int, endLine: Int) {
        cache.updateForDeletion(startLine, endLine)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            renderNodeHolder?.afterDelete(startLine, endLine)
        }
    }

    fun reset(lineCount: Int) {
        cache.reset(lineCount)
    }

}
