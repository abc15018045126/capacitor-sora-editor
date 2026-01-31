
package io.github.abc15018045126.sora.util;

import android.util.Log;

import java.util.Map;
import java.util.WeakHashMap;

public class Logger {

    private static final Map<String, Logger> map = new WeakHashMap<>();
    private final String name;

    private Logger(String name) {
        this.name = name;
    }

    public synchronized static Logger instance(String name) {
        var logger = map.get(name);
        if (logger == null) {
            logger = new Logger(name);
            map.put(name, logger);
        }
        return logger;
    }

    public void d(String msg) {
        Log.d(name, msg);
    }

    public void d(String msg, Object... format) {
        Log.d(name, String.format(msg, format));
    }

    public void i(String msg) {
        Log.i(name, msg);
    }

    public void i(String msg, Object... format) {
        Log.i(name, String.format(msg, format));
    }

    public void v(String msg) {
        Log.v(name, msg);
    }

    public void v(String msg, Object... format) {
        Log.v(name, String.format(msg, format));
    }

    public void w(String msg) {
        Log.w(name, msg);
    }


    public void w(String msg, Object... format) {
        Log.w(name, String.format(msg, format));
    }

    public void w(String msg, Throwable e) {
        Log.w(name, msg, e);
    }

    public void w(String msg, Throwable e, Object... format) {
        Log.w(name, String.format(msg, format), e);
    }

    public void e(String msg) {
        Log.e(name, msg);
    }

    public void e(String msg, Object... format) {
        Log.e(name, String.format(msg, format));
    }


    public void e(String msg, Throwable e) {
        Log.e(name, msg, e);
    }

    public void e(String msg, Throwable e, Object... format) {
        Log.e(name, String.format(msg, format), e);
    }


}

