
package io.github.abc15018045126.sora.text.breaker;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.ContentLine;

/**
 * Breakpoint optimizer used when breaking text to visual rows
 */
public interface WordBreaker {

    int getOptimizedBreakPoint(int start, int end);

    class Factory {

        @NonNull
        public static WordBreaker newInstance(@NonNull ContentLine text) {
            return new WordBreakerProgram(text);
        }

    }

}

