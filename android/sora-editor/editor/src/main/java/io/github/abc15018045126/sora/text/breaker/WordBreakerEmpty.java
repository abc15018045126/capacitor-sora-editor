
package io.github.abc15018045126.sora.text.breaker;

public class WordBreakerEmpty implements WordBreaker {

    public static WordBreaker INSTANCE = new WordBreakerEmpty();

    private WordBreakerEmpty() {

    }

    @Override
    public int getOptimizedBreakPoint(int start, int end) {
        return end;
    }
}
