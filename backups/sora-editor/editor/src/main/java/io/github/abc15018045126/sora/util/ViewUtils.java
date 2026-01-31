
package io.github.abc15018045126.sora.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.Log;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

public class ViewUtils {

    private final static String LOG_TAG = "ViewUtils";

    public final static float DEFAULT_SCROLL_FACTOR = 32f;

    public final static long HOVER_TOOLTIP_SHOW_TIMEOUT = 1000;

    public static final int HOVER_TAP_SLOP = 20;

    public static float getVerticalScrollFactor(@NonNull Context context) {
        float verticalScrollFactor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var configuration = ViewConfiguration.get(context);
            verticalScrollFactor = configuration.getScaledVerticalScrollFactor();
        } else {
            TypedArray a = null;
            try {
                a = context.obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemHeight});
                verticalScrollFactor = a.getDimension(0, DEFAULT_SCROLL_FACTOR);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to get vertical scroll factor, using default.", e);
                verticalScrollFactor = DEFAULT_SCROLL_FACTOR;
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }
        return verticalScrollFactor;
    }

}

