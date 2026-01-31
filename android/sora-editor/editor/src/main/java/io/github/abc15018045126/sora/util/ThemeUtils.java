
package io.github.abc15018045126.sora.util;

import android.util.TypedValue;
import android.view.ContextThemeWrapper;

public class ThemeUtils {

    public static int getColorPrimary(ContextThemeWrapper context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        return typedValue.data;
    }

}

