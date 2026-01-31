
package io.github.abc15018045126.sora.lang.smartEnter;

public class NewlineHandleResult {

    /**
     * Text to insert
     */
    public final CharSequence text;

    /**
     * Count to shift left from the end of {@link NewlineHandleResult#text}
     */
    public final int shiftLeft;

    public NewlineHandleResult(CharSequence text, int shiftLeft) {
        this.text = text;
        this.shiftLeft = shiftLeft;
        if (shiftLeft < 0 || shiftLeft > text.length()) {
            throw new IllegalArgumentException("invalid shiftLeft");
        }
    }

}

