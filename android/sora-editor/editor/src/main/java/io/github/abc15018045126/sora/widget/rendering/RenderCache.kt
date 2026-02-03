

package io.github.abc15018045126.sora.widget.rendering

import androidx.collection.MutableIntList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class RenderCache {
    private val lines = MutableIntList()
    private val cache = mutableListOf<MeasureCacheItem>()
    private var maxCacheCount = 75

    fun getOrCreateMeasureCache(line: Int): MeasureCacheItem {
        return queryMeasureCache(line) ?: run {
            MeasureCacheItem(line, null, 0L).also {
                cache.add(it)
                while (cache.size > maxCacheCount && cache.isNotEmpty()) {
                    cache.removeAt(0)
                }
            }
        }
    }

    fun queryMeasureCache(line: Int) =
        cache.firstOrNull { it.line == line }.also {
            if (it != null) {
                cache.remove(it)
                cache.add(it)
            }
        }


    fun getStyleHash(line: Int) = lines[line]

    fun setStyleHash(line: Int, hash: Int) {
        lines[line] = hash
    }

    fun updateForInsertion(startLine: Int, endLine: Int) {
        if (startLine != endLine) {
            if (endLine - startLine == 1) {
                lines.add(startLine, 0)
            } else {
                lines.addAll(startLine, IntArray(endLine - startLine))
            }
            cache.forEach {
                if (it.line > startLine) {
                    it.line += endLine - startLine
                }
            }
        }
    }

    fun updateForDeletion(startLine: Int, endLine: Int) {
        if (startLine != endLine) {
            lines.removeRange(startLine, endLine)
            cache.removeAll { it.line in startLine..endLine }
            cache.forEach {
                if (it.line > endLine) {
                    it.line -= endLine - startLine
                }
            }
        }
    }

    fun reset(lineCount: Int) {
        if (lines.size > lineCount) {
            lines.removeRange(lineCount, lines.size)
        } else if (lines.size < lineCount) {
            repeat(lineCount - lines.size) {
                lines.add(0)
            }
        }
        lines.indices.forEach { lines[it] = 0 }
        cache.clear()
    }

}
