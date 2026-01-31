
package io.github.abc15018045126.sora.langs.textmate.folding;

public class FoldingRegion {

    private final FoldingRegions ranges;
    private final int index;

    public FoldingRegion(FoldingRegions ranges, int index) {
        this.ranges = ranges;
        this.index = index;
    }

    public int getStartLineNumber() {
        return this.ranges.getStartLineNumber(this.index);
    }

    public int getEndLineNumber() {
        return this.ranges.getEndLineNumber(this.index);
    }

    public int getRegionIndex() {
        return this.index;
    }
    public int getParentIndex() throws Exception {
        return this.ranges.getParentIndex(this.index);
    }
}

