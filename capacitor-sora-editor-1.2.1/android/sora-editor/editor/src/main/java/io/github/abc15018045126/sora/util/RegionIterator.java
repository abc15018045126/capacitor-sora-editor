
package io.github.abc15018045126.sora.util;

import androidx.annotation.NonNull;

/**
 * This class provides region division and iteration with several {@link RegionProvider}s.
 *
 * @author abc15018045126
 */
public class RegionIterator {

    private final int max;
    private final RegionProvider[] providers;
    private final int[] pointers;
    private final boolean[] pointerStates;
    protected int startIndex;
    protected int endIndex;

    /**
     * Creates a new {@link RegionIterator} with the given params
     * @param max The length of full region, that's [0,max-1]
     * @param providers Providers of dividing points
     */
    public RegionIterator(int max, @NonNull RegionProvider... providers) {
        this.max = max;
        this.providers = providers;
        pointers = new int[providers.length];
        pointerStates = new boolean[providers.length];
    }

    /**
     * Move to next region
     */
    public void nextRegion() {
        startIndex = endIndex;
        int minNext = max;
        for (int i = 0; i < providers.length; i++) {
            int next, value;
            if (pointers[i] < providers[i].getPointCount() && (value = providers[i].getPointAt(pointers[i])) <= max) {
                next = value;
            } else {
                next = max;
            }
            minNext = Math.min(next, minNext);
        }
        endIndex = minNext;
        for (int i = 0; i < providers.length; i++) {
            if (pointers[i] < providers[i].getPointCount() && providers[i].getPointAt(pointers[i]) == minNext) {
                pointers[i]++;
                pointerStates[i] = true;
            } else {
                pointerStates[i] = false;
            }
        }
    }

    /**
     * Check if we can move to next region
     */
    public boolean hasNextRegion() {
        return endIndex < max;
    }

    /**
     * Get current index of dividing points in provider with given index {@code i}
     * @param i Index of provider
     * @return Current index of regions in that provider
     */
    public int getPointer(int i) {
        return pointers[i];
    }

    /**
     * Get the source index of dividing points in provider with given index {@code i}.
     * Source index is the index of dividing point that leads to current region.
     * @param i Index of provider
     */
    public int getRegionSourcePointer(int i) {
        var pointerValue = pointers[i] < providers[i].getPointCount() ? providers[i].getPointAt(i) : max;
        return (endIndex <= pointerValue && pointerValue < max || pointerStates[i]) ? pointers[i] - 1 : pointers[i];
    }

    public int getPointerValue(int i, int j) {
        var provider = providers[i];
        if (j < 0) {
            return 0;
        }
        if (j >= provider.getPointCount()) {
            return max;
        }
        var value = provider.getPointAt(j);
        return Math.min(value, max);
    }

    /**
     * Get start index of the region
     * Note that this is inclusive.
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * Get end index of the region.
     * Note that this is exclusive.
     */
    public int getEndIndex() {
        return Math.min(endIndex, max);
    }

    /**
     * Get length of the full region.
     */
    public int getMax() {
        return max;
    }

    /**
     * RegionProvider provides dividing points for {@link RegionIterator}. Note that the returned
     * sequence must follow a ascent order.
     */
    public interface RegionProvider {

        /**
         * Get count of dividing points
         */
        int getPointCount();

        /**
         * Get get dividing point at given index
         * @param index Index of point
         * @return Dividing index in region
         */
        int getPointAt(int index);

    }

}

