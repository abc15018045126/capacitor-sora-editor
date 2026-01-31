
package io.github.abc15018045126.sora.text.bidi;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.util.IntPair;

/**
 * Manages directions in a text segment
 *
 * @author abc15018045126
 */
public class Directions implements IDirections {

    private long[] runs;
    private int length;

    public Directions(@NonNull long[] runs, int length) {
        this.runs = runs;
        this.length = length;
    }

    public void setData(@NonNull long[] runs, int length) {
        this.runs = runs;
        this.length = length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public int getRunCount() {
        return runs.length;
    }

    public int getRunStart(int i) {
        return IntPair.getFirst(runs[i]);
    }

    public int getRunEnd(int i) {
        return i == runs.length - 1 ? length : getRunStart(i + 1);
    }

    public int getRunLevel(int i) {
        return IntPair.getSecond(runs[i]);
    }

    public boolean isRunRtl(int i) {
        return (getRunLevel(i) & 1) == 1;
    }

}

