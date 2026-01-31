
package io.github.abc15018045126.sora.text;

import android.icu.text.BreakIterator;
import android.os.Build;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.util.IntPair;
import io.github.abc15018045126.sora.util.MyCharacter;

/**
 * Utility class for invoking ICU library according to Android platform.
 * Provide default implementation on old platforms without ICU.
 *
 * @author abc15018045126
 */
public class ICUUtils {

    /**
     * Get word range for the given offset
     *
     * @param text   Text to analyze
     * @param offset Required char offset of word
     * @return Packed integer pair (start, end). Always contains the position {@code offset}
     */
    public static long getWordRange(@NonNull CharSequence text, int offset, boolean useIcu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && useIcu) {
            var itr = BreakIterator.getWordInstance();
            itr.setText(new CharSequenceIterator(text));
            int end = itr.following(offset);
            int start = itr.previous();
            if (offset >= start && offset <= end) {
                return IntPair.pack(start, end);
            } else {
                return getWordRangeFallback(text, offset);
            }
        } else {
            return getWordRangeFallback(text, offset);
        }
    }

    /**
     * Primitive implementation of {@link #getWordRange(CharSequence, int, boolean)}
     * when ICU is not enabled
     */
    public static long getWordRangeFallback(@NonNull CharSequence text, int offset) {
        int start = offset;
        int end = offset;
        while (end < text.length() && MyCharacter.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        if (end > offset) {
            while (start > 0 && MyCharacter.isJavaIdentifierPart(text.charAt(start - 1))) {
                start--;
            }
        }
        return IntPair.pack(start, end);
    }


}

