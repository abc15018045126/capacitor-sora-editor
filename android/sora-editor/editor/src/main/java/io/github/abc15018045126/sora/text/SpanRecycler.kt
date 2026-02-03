package io.github.abc15018045126.sora.text

import android.util.Log
import io.github.abc15018045126.sora.lang.styling.Span
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class SpanRecycler private constructor() {

    private val taskQueue: BlockingQueue<MutableList<Span>> = ArrayBlockingQueue(8)
    private var recycleThread: RecycleThread? = null

    fun recycle(spans: MutableList<Span>?) {
        if (spans == null) {
            return
        }
        if (recycleThread == null || !recycleThread!!.isAlive) {
            recycleThread = RecycleThread()
            recycleThread!!.start()
        }
        taskQueue.offer(spans)
    }

    private inner class RecycleThread internal constructor() : Thread() {

        init {
            isDaemon = true
            name = "SpanRecycleDaemon"
        }

        override fun run() {
            try {
                while (!isInterrupted) {
                    try {
                        val spans = taskQueue.take()
                        var count = 0
                        val size = spans.size
                        for (i in 0 until size) {
                            val span = spans[i]
                            if (!span.recycle()) {
                                // If recycle returns false (e.g. pool full), we stop? 
                                // Original logic stopped at first failure. 
                                // But original logic was removing from end. 
                                // spans.removeAt(size - 1 - i)
                                // If I iterate forward, I might want to clear ALL? 
                                // But if recycle() fails, maybe it means pool is full. 
                                // We can just clear the rest.
                                break
                            }
                            count++
                        }
                        spans.clear()
                        Log.i(LOG_TAG, "Called recycle() on $count spans")
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
            }
            Log.i(LOG_TAG, "Recycler exited")
        }
    }

    companion object {
        private const val LOG_TAG = "SpanRecycler"
        private var INSTANCE: SpanRecycler? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): SpanRecycler {
            if (INSTANCE == null) {
                INSTANCE = SpanRecycler()
            }
            return INSTANCE!!
        }
    }
}
