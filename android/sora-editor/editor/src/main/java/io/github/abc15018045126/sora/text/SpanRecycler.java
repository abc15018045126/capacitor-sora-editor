
package io.github.abc15018045126.sora.text;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.github.abc15018045126.sora.lang.styling.Span;

public class SpanRecycler {

    private static SpanRecycler INSTANCE;
    private final BlockingQueue<List<Span>> taskQueue;
    private Thread recycleThread;

    private SpanRecycler() {
        taskQueue = new ArrayBlockingQueue<>(8);
    }

    public static synchronized SpanRecycler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpanRecycler();
        }
        return INSTANCE;
    }

    public void recycle(@Nullable List<Span> spans) {
        if (spans == null) {
            return;
        }
        if (recycleThread == null || !recycleThread.isAlive()) {
            recycleThread = new RecycleThread();
            recycleThread.start();
        }
        taskQueue.offer(spans);
    }

    private class RecycleThread extends Thread {

        private final static String LOG_TAG = "SpanRecycler";

        RecycleThread() {
            setDaemon(true);
            setName("SpanRecycleDaemon");
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    try {
                        var spans = taskQueue.take();
                        int count = 0;
                        int size = spans.size();
                        for (int i = 0; i < size; i++) {
                            var recycled = spans.remove(size - 1 - i).recycle();
                            if (!recycled) {
                                break;
                            }
                            count++;
                        }
                        Log.i(LOG_TAG, "Called recycle() on " + count + " spans");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
            Log.i(LOG_TAG, "Recycler exited");
        }

    }

}

