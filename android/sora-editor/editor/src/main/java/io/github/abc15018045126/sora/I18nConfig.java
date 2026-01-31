
package io.github.abc15018045126.sora;

import android.content.Context;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

/**
 * Map editor built-in string resources to your given string resource. Editor string resource has
 * limited i18n function, as it only contains English and Chinese.
 * <p>
 * Note that you should configure this before creating editor instances
 *
 * @author abc15018045126
 */
public class I18nConfig {

    private static final SparseIntArray mapping = new SparseIntArray();

    /**
     * Map the given editor resId to new one
     */
    public static void mapTo(int originalResId, int newResId) {
        mapping.put(originalResId, newResId);
    }

    /**
     * Get mapped resource id or itself
     */
    public static int getResourceId(int resId) {
        int newResource = mapping.get(resId);
        if (newResource == 0) {
            return resId;
        }
        return newResource;
    }

    /**
     * Get mapped resource string
     */
    public static String getString(@NonNull Context context, int resId) {
        return context.getString(getResourceId(resId));
    }

}

