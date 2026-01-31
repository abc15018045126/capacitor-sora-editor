
package io.github.abc15018045126.sora.graphics;

/**
 * Utility for breaking text by grapheme bounds
 *
 * @author abc15018045126
 */
public class GraphemeBoundsBreaker {

    /**
     * Find next grapheme break point before the given width
     */
    public static int findGraphemeBreakPoint(float[] advances, int length, int width, int start) {
        float currentWidth = 0;
        int next = start;
        while (next < length) {
            if (advances[next] == 0) {
                // Not grapheme bound
                next++;
                continue;
            }
            if (currentWidth + advances[next] > width) {
                break;
            }
            currentWidth += advances[next];
            next++;
        }
        return next;
    }

}

