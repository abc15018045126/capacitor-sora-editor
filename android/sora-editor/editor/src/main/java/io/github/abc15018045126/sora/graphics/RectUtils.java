
package io.github.abc15018045126.sora.graphics;

import android.graphics.RectF;

public class RectUtils {

    public static boolean contains(RectF rect, float x, float y, float extraXSpace) {
        return (x >= rect.left - extraXSpace && x <= rect.right + extraXSpace && y >= rect.top && y <= rect.bottom);
    }

    public static boolean almostContains(RectF rect, float x, float y, float extraSpace) {
        return (x >= rect.left - extraSpace && x <= rect.right + extraSpace && y >= rect.top - extraSpace && y <= rect.bottom + extraSpace);
    }

}

