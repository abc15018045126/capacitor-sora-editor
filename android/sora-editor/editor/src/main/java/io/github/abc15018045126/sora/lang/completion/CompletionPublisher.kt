package io.github.abc15018045126.sora.lang.completion

import android.os.Handler
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import io.github.abc15018045126.sora.lang.Language


class CompletionPublisher(
    private val handler: Handler,
    private val callback: Runnable,
    private val languageInterruptionLevel: Int
) {

    companion object {

        const val DEFAULT_UPDATE_THRESHOLD = 5
    }

    private val items: MutableList<CompletionItem> = ArrayList()
    private val candidates: MutableList<CompletionItem> = ArrayList()
    private val lock: Lock = ReentrantLock(true)
    private var comparator: Comparator<CompletionItem>? = null
    private var updateThreshold: Int = DEFAULT_UPDATE_THRESHOLD
    private var invalid = false


    val isCancelled: Boolean
        get() = invalid


    fun hasData(): Boolean {
        return items.size + candidates.size > 0
    }


    @io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
    fun getItems(): List<CompletionItem> {
        return items
    }


    fun setUpdateThreshold(updateThreshold: Int) {
        this.updateThreshold = updateThreshold
    }


    fun setComparator(comparator: Comparator<CompletionItem>?) {
        checkCancelled()
        if (invalid) {
            return
        }
        this.comparator = comparator
        if (items.isNotEmpty() && comparator != null) {
            handler.post {
                if (invalid) {
                    return@post
                }
                Collections.sort(items, comparator)
                callback.run()
            }
        }
    }


    fun addItems(items: Collection<CompletionItem>) {
        checkCancelled()
        if (invalid) {
            return
        }
        lock.lock()
        try {
            candidates.addAll(items)
        } finally {
            lock.unlock()
        }
        if (candidates.size >= updateThreshold) {
            updateList()
        }
    }


    fun addItem(item: CompletionItem) {
        checkCancelled()
        if (invalid) {
            return
        }
        lock.lock()
        try {
            candidates.add(item)
        } finally {
            lock.unlock()
        }
        if (candidates.size >= updateThreshold) {
            updateList()
        }
    }


    fun updateList() {
        updateList(false)
    }


    fun updateList(forced: Boolean) {
        if (invalid) {
            return
        }
        handler.post {

            if (invalid) {
                callback.run()
                return@post
            }
            var locked = false
            if (forced) {
                lock.lock()
                locked = true
            } else {
                locked = lock.tryLock()
            }

            if (locked) {
                try {
                    if (candidates.isEmpty()) {
                        callback.run()
                        return@post
                    }
                    val comparator = this.comparator
                    if (comparator != null) {
                        while (candidates.isNotEmpty()) {
                            val candidate = candidates.removeAt(0)

                            var left = 0
                            var right = items.size
                            val size = right
                            while (left <= right) {
                                val mid = (left + right) / 2
                                if (mid < 0 || mid >= size) {
                                    left = mid
                                    break
                                }
                                val cmp = comparator.compare(items[mid], candidate)
                                if (cmp < 0) {
                                    left = mid + 1
                                } else if (cmp > 0) {
                                    right = mid - 1
                                } else {
                                    left = mid
                                    break
                                }
                            }
                            left = kotlin.math.max(0, kotlin.math.min(size, left))
                            items.add(left, candidate)
                        }
                    } else {
                        items.addAll(candidates)
                        candidates.clear()
                    }
                    callback.run()
                } finally {
                    lock.unlock()
                }
            }
        }
    }


    fun cancel() {
        invalid = true
    }


    fun checkCancelled() {
        if (Thread.interrupted() || invalid) {
            invalid = true
            if (languageInterruptionLevel <= Language.INTERRUPTION_LEVEL_SLIGHT) {
                throw CompletionCancelledException()
            }
        }
    }
}
