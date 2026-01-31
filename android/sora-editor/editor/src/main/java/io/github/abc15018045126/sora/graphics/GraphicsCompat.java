
package io.github.abc15018045126.sora.graphics;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.os.Build;

import androidx.annotation.NonNull;

public class GraphicsCompat {

    /**
     * {@link Canvas#drawTextRun(char[], int, int, int, int, float, float, boolean, android.graphics.Paint)} is also available on API 21 & 22,
     * but with hidden access.
     * As there is no hidden list checks in those API platforms, it's safe here to call the "New API".
     */
    @SuppressLint("NewApi")
    public static void drawTextRun(Canvas canvas, @NonNull char[] text, int index, int count, int contextIndex,
                                   int contextCount, float x, float y, boolean isRtl, @NonNull android.graphics.Paint paint) {
        canvas.drawTextRun(text, index, count, contextIndex, contextCount, x, y, isRtl, paint);
    }

    public static float getRunAdvance(Paint paint, char[] text, int start, int end, int contextStart, int contextEnd,
                                      boolean isRtl, int offset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return paint.getRunAdvance(text, start, end, contextStart, contextEnd, isRtl, offset);
        } else {
            return paint.measureTextRunAdvance(text, start, offset, contextStart, contextEnd, isRtl);
        }
    }

}

