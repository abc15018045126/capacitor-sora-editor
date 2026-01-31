
package io.github.abc15018045126.sora.lang.completion;

import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.ContentReference;
import io.github.abc15018045126.sora.widget.component.EditorAutoCompletion;

/**
 * Helper class for completion
 *
 * @author abc15018045126
 */
public class CompletionHelper {

    /**
     * Searches backward on the line, with the given checker to check chars.
     * Returns the longest text that matches the requirement
     */
    public static String computePrefix(ContentReference ref, CharPosition pos, PrefixChecker checker) {
        int begin = pos.column;
        var line = ref.getLine(pos.line);
        for (; begin > 0; begin--) {
            if (!checker.check(line.charAt(begin - 1))) {
                break;
            }
        }
        return line.substring(begin, pos.column);
    }

    /**
     * Check whether the thread is abandoned by editor.
     * Return true if it is cancelled by editor.
     */
    public static boolean checkCancelled() {
        var thread = Thread.currentThread();
        if (thread instanceof EditorAutoCompletion.CompletionThread) {
            return ((EditorAutoCompletion.CompletionThread) thread).isCancelled();
        } else {
            return false;
        }
    }

    public interface PrefixChecker {

        boolean check(char ch);

    }

}

