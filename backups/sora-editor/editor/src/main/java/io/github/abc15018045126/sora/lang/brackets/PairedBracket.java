
package io.github.abc15018045126.sora.lang.brackets;

/**
 * Describes paired brackets
 *
 * @author abc15018045126
 */
public class PairedBracket {

    public final int leftIndex, leftLength, rightIndex, rightLength;

    /**
     * Currently length is always 1.
     *
     * @see #PairedBracket(int, int, int, int)
     */
    public PairedBracket(int leftIndex, int rightIndex) {
        this(leftIndex, 1, rightIndex, 1);
    }

    /**
     * @param leftIndex   Index of left bracket in text
     * @param leftLength  Text length of left bracket
     * @param rightIndex  Index of right bracket in text
     * @param rightLength Text length of right bracket
     */
    public PairedBracket(int leftIndex, int leftLength, int rightIndex, int rightLength) {
        this.leftIndex = leftIndex;
        this.leftLength = leftLength;
        this.rightIndex = rightIndex;
        this.rightLength = rightLength;
    }
}

