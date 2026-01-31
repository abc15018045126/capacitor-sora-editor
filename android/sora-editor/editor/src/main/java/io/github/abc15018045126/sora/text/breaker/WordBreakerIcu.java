
package io.github.abc15018045126.sora.text.breaker;

import androidx.annotation.NonNull;

import java.text.BreakIterator;

import io.github.abc15018045126.sora.text.CharSequenceIterator;
import io.github.abc15018045126.sora.text.ContentLine;

public class WordBreakerIcu implements WordBreaker {

    protected final BreakIterator wrappingIterator;

    protected final char[] chars;

    public WordBreakerIcu(@NonNull ContentLine text) {
        this.chars = text.getBackingCharArray();
        var textIterator = new CharSequenceIterator(text);
        wrappingIterator = BreakIterator.getLineInstance();
        wrappingIterator.setText(textIterator);
    }

    public int getOptimizedBreakPoint(int start, int end) {
        // Merging trailing whitespaces is not supported by editor, so force to break here
        if (end > 0 && !Character.isWhitespace(chars[end - 1]) && !wrappingIterator.isBoundary(end)) {
            // Break text at last boundary
            int lastBoundary = wrappingIterator.preceding(end);
            if (lastBoundary != BreakIterator.DONE) {
                int suggestedNext = Math.max(start, Math.min(end, lastBoundary));
                if (suggestedNext > start) {
                    end = suggestedNext;
                }
            }
        }
        return end;
    }

}

