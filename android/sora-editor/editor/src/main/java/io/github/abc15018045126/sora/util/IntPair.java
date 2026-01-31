
package io.github.abc15018045126.sora.util;

/**
 * Pack two int numbers into a long number, and unpack it.
 * <p>
 * This is effective for passing two primitive 32-bit numbers without creating a new object.
 *
 * @author abc15018045126
 */
public class IntPair {

    /**
     * Convert an integer to a long whose binary bits are equal to the given integer
     */
    private static long toUnsignedLong(int x) {
        return ((long) x) & 0xffffffffL;
    }

    /**
     * Pack two int number into a long number
     *
     * @param first  First of pair
     * @param second Second of pair
     * @return Packed value
     */
    public static long pack(int first, int second) {
        return (toUnsignedLong(first) << 32L) | toUnsignedLong(second);
    }

    /**
     * Get second of pair
     *
     * @param packedValue Packed value
     * @return Second of pair
     */
    public static int getSecond(long packedValue) {
        return (int) (packedValue & 0xFFFFFFFFL);
    }

    /**
     * Get first of pair
     *
     * @param packedValue Packed value
     * @return First of pair
     */
    public static int getFirst(long packedValue) {
        return (int) (packedValue >> 32L);
    }

    /**
     * Pack an int number and a floating-number into a long number
     *
     * @param first  First of pair
     * @param second Second of pair (float)
     * @return Packed value
     */
    public static long packIntFloat(int first, float second) {
        return pack(first, Float.floatToRawIntBits(second));
    }

    /**
     * Get second of pair, but as a floating number
     *
     * @param packedValue Packed value
     * @return Second of pair
     * @see #packIntFloat(int, float)
     */
    public static float getSecondAsFloat(long packedValue) {
        return Float.intBitsToFloat(getSecond(packedValue));
    }

}

