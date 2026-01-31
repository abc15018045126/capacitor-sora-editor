
package io.github.abc15018045126.sora.text.breaker;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.ContentLine;

public class WordBreakerProgram extends WordBreakerIcu {

    public WordBreakerProgram(@NonNull ContentLine text) {
        super(text);
    }

    @Override
    public int getOptimizedBreakPoint(int start, int end) {
        int icuResult = super.getOptimizedBreakPoint(start, end);
        if (icuResult != end || end <= start || /* end > start */ Character.isWhitespace(chars[end - 1])) {
            return icuResult;
        }
        // Add extra opportunities for dots
        int index = end - 1;
        while (index > start) {
            if (chars[index] == '.' && index - 1 >= start && !Character.isDigit(chars[index - 1])) {
                // Break after this dot
                return index + 1;
            }
            index--;
        }
        return end;
    }
}

