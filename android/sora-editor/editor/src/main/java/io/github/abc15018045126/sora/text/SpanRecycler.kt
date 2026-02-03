package io.github.abc15018045126.sora.text

import android.util.Log
import io.github.abc15018045126.sora.lang.styling.Span
import java.util.concurrent.ArrayBlockingQueue

class SpanRecycler private constructor() {
    private val taskQueue = ArrayBlockingQueue<MutableList<Span>>(8)
    private var recycleThread: Thread? = null

    fun recycle(spans: MutableList<Span>?) {
        if (spans == null) return
        if (recycleThread?.isAlive != true) {
            recycleThread = Thread {
                try {
                    while (!Thread.interrupted()) {
                        val list = taskQueue.take()
                        var count = 0
                        for (s in list) if (s.recycle()) count++ else break
                        list.clear()
                        Log.i(LOG_TAG, "Recycled $count spans")
                    }
                } catch (e: Exception) { Log.w(LOG_TAG, e) }
            }.apply { isDaemon = true; name = "SpanRecycleDaemon"; start() }
        }
        taskQueue.offer(spans)
    }

    companion object {
        private const val LOG_TAG = "SpanRecycler"
        private val INSTANCE by lazy { SpanRecycler() }
        @JvmStatic fun getInstance() = INSTANCE
    }
}
