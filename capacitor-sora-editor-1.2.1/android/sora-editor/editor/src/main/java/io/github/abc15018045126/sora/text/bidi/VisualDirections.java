
package io.github.abc15018045126.sora.text.bidi;


import androidx.annotation.NonNull;


import java.text.Bidi;

import io.github.abc15018045126.sora.util.IntPair;

/**
 * Helper class for reordering logical text runs to visual runs.
 *
 * @author abc15018045126
 */
public class VisualDirections implements IDirections {

    private final RunInfo[] runs;

    private static class RunInfo {
        long range;
        int level;

        public RunInfo(long range, int level) {
            this.range = range;
            this.level = level;
        }
    }


    public VisualDirections(@NonNull Directions dirs) {
        int runCount = dirs.getRunCount();
        runs = new RunInfo[runCount];
        var paramLevels = new byte[runCount];
        for (int i = 0; i < runCount; i++) {
            paramLevels[i] = (byte) dirs.getRunLevel(i);
            runs[i] = new RunInfo(IntPair.pack(dirs.getRunStart(i), dirs.getRunEnd(i)), dirs.getRunLevel(i));
        }
        Bidi.reorderVisually(paramLevels, 0, runs, 0, runCount);
    }


    public int getRunCount() {
        return runs.length;
    }

    public int getRunStart(int i) {
        return IntPair.getFirst(runs[i].range);
    }

    public int getRunEnd(int i) {
        return IntPair.getSecond(runs[i].range);
    }

    public int getRunLevel(int i) {
        return runs[i].level;
    }

    public boolean isRunRtl(int i) {
        return (getRunLevel(i) & 1) != 0;
    }


}

