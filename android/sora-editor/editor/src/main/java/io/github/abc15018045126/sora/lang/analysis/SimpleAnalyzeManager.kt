package io.github.abc15018045126.sora.lang.analysis

import android.os.Bundle
import android.util.Log
import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference


abstract class SimpleAnalyzeManager<V> : AnalyzeManager {

    private val lock = Object()

    override var receiver: StyleReceiver? = null

    @Volatile
    private var ref: ContentReference? = null


    var extraArguments: Bundle? = null
        private set

    @Volatile
    private var newestRequestId: Long = 0
    private var thread: AnalyzeThread? = null


    var data: V? = null
        private set

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.ref = content
        this.extraArguments = extraArguments
        rerun()
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        rerun()
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        rerun()
    }

    @Synchronized
    override fun rerun() {
        newestRequestId++
        val currentThread = thread
        if (currentThread == null || !currentThread.isAlive) {

            Log.v(LOG_TAG, "Starting a new thread for analysis")
            val newThread = AnalyzeThread()
            newThread.isDaemon = true
            newThread.name = "SplAnalyzer-" + nextThreadId()
            newThread.start()
            thread = newThread
        }
        synchronized(lock) {
            lock.notify()
        }
    }

    override fun destroy() {
        ref = null
        extraArguments = null
        newestRequestId = 0
        data = null
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
            }
        }
        thread = null
        receiver = null
    }


    protected abstract fun analyze(text: StringBuilder, delegate: Delegate<V>): Styles?


    private inner class AnalyzeThread : Thread() {


        private val textContainer = StringBuilder()

        override fun run() {
            Log.v(LOG_TAG, "Analyze thread started")
            try {
                while (!isInterrupted) {
                    val text = ref
                    if (text != null) {
                        var requestId: Long
                        var result: Styles? = null
                        var newData: V? = null

                        do {
                            val currentText = ref ?: break
                            requestId = newestRequestId
                            val delegate = Delegate<V>(requestId)


                            textContainer.setLength(0)
                            textContainer.ensureCapacity(currentText.lineCount * 10)
                            for (i in 0 until currentText.lineCount) {
                                if (requestId != newestRequestId) break
                                if (i != 0) {
                                    textContainer.append(currentText.getLineSeparator(i - 1))
                                }
                                currentText.appendLineTo(textContainer, i)
                            }

                            if (requestId != newestRequestId) {
                                result = null
                                newData = null
                                continue
                            }


                            result = analyze(textContainer, delegate)
                            newData = delegate.data
                        } while (requestId != newestRequestId)


                        result?.let {
                            receiver?.setStyles(this@SimpleAnalyzeManager, it)
                        }
                        this@SimpleAnalyzeManager.data = newData
                    }

                    synchronized(lock) {
                        lock.wait()
                    }
                }
            } catch (e: InterruptedException) {
                Log.v(LOG_TAG, "Thread is interrupted.")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Unexpected exception is thrown in the thread.", e)
            }
        }
    }


    inner class Delegate<T>(private val myRequestId: Long) {
        var data: T? = null


        fun isCancelled(): Boolean {
            return myRequestId != newestRequestId
        }
    }

    companion object {
        private const val LOG_TAG = "SimpleAnalyzeManager"
        private var sThreadId = 0

        @Synchronized
        private fun nextThreadId(): Int {
            sThreadId++
            return sThreadId
        }
    }
}
