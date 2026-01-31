
package io.github.abc15018045126.sora.text.bidi;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.text.Bidi;

import io.github.abc15018045126.sora.util.IntPair;
import io.github.abc15018045126.sora.util.TemporaryCharBuffer;

/**
 * Text bidirectional utils. Some codes are from AOSP
 *
 * @author abc15018045126
 */
public class TextBidi {

    /**
     * Compute text directions for the given text
     */
    @NonNull
    public static Directions getDirections(@NonNull CharSequence text) {
        var len = text.length();
        if (doesNotNeedBidi(text)) {
            return new Directions(new long[]{IntPair.pack(0, 0)}, len);
        }
        var chars = TemporaryCharBuffer.obtain(len);
        TextUtils.getChars(text, 0, len, chars, 0);
        var bidi = new Bidi(chars, 0, null, 0, text.length(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        var runs = new long[bidi.getRunCount()];
        for (int i = 0; i < runs.length; i++) {
            runs[i] = IntPair.pack(bidi.getRunStart(i), bidi.getRunLevel(i));
        }
        TemporaryCharBuffer.recycle(chars);
        return new Directions(runs, len);
    }

    public static boolean couldAffectRtl(char c) {
        return (0x0590 <= c && c <= 0x08FF) ||  // RTL scripts
                c == 0x200E ||  // Bidi format character
                c == 0x200F ||  // Bidi format character
                (0x202A <= c && c <= 0x202E) ||  // Bidi format characters
                (0x2066 <= c && c <= 0x2069) ||  // Bidi format characters
                (0xD800 <= c && c <= 0xDFFF) ||  // Surrogate pairs
                (0xFB1D <= c && c <= 0xFDFF) ||  // Hebrew and Arabic presentation forms
                (0xFE70 <= c && c <= 0xFEFE);  // Arabic presentation forms
    }

    /**
     * Returns true if there is no character present that may potentially affect RTL layout.
     * Since this calls couldAffectRtl() above, it's also quite conservative, in the way that
     * it may return 'false' (needs bidi) although careful consideration may tell us it should
     * return 'true' (does not need bidi).
     */
    public static boolean doesNotNeedBidi(@NonNull CharSequence text) {
        if (text instanceof BidiRequirementChecker) {
            return !((BidiRequirementChecker) text).mayNeedBidi();
        }
        final var len = text.length();
        for (int i = 0; i < len; i++) {
            if (couldAffectRtl(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}

