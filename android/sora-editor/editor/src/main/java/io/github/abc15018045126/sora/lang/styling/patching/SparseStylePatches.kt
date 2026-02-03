

package io.github.abc15018045126.sora.lang.styling.patching

import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.Arrays
import java.util.Collections


class SparseStylePatches {

    private val patches = mutableListOf<StylePatch>()

    private var immutable = false

    private fun getInsertionPoint(patch: StylePatch): Int {
        val result = patches.binarySearch(patch)
        val insertionPoint = if (result < 0) {
            -(result + 1)
        } else {
            result
        }
        return insertionPoint
    }

    fun addPatch(patch: StylePatch) {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        if (patch.startLine != patch.endLine) throw UnsupportedOperationException("crossline patch is not supported now")
        patches.add(getInsertionPoint(patch), patch)
    }

    fun setImmutable() {
        immutable = true
    }

    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val coordinator = StylePatch(startLine, 0, startLine, 0)
        var index = getInsertionPoint(coordinator)
        val delta = endLine - startLine
        while (index < patches.size) {
            val e = patches[index++]
            if (e.startLine == startLine && e.startColumn >= startColumn) {
                val length = e.endColumn - e.startColumn
                e.startLine = endLine
                e.endLine = endLine
                e.startColumn = endColumn + (e.startColumn - startColumn)
                e.endColumn = e.startColumn + length
            } else if (e.startLine > startLine) {
                if (delta == 0) break
                e.startLine += delta
                e.endLine += delta
            }
        }
    }

    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val coordinator = StylePatch(startLine, 0, startLine, 0)
        var index = getInsertionPoint(coordinator)
        val delta = endLine - startLine
        while (index < patches.size) {
            val e = patches[index]

            if (e.startLine < endLine || (e.startLine == endLine && e.endColumn < endColumn)) {

            }
            index++
        }
    }

}
