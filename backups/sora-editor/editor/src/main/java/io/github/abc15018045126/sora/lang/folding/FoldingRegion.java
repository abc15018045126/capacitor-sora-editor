
package io.github.abc15018045126.sora.lang.folding;

import java.util.List;

import io.github.abc15018045126.sora.util.IntPair;

/**
 * Indicates a folding region
 */
public class FoldingRegion {

    private long start;
    private long end;
    private boolean collapsed;
    private List<FoldingRegion> children;

    public FoldingRegion(int startLine, int startColumn, int endLine, int endColumn) {
        this(IntPair.pack(startLine, startColumn), IntPair.pack(endLine, endColumn));
        if (startLine > endLine || (startLine == endLine && startColumn > endColumn)) {
            throw new IllegalArgumentException("start > end");
        }
    }

    FoldingRegion(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public int getStartLine() {
        return IntPair.getFirst(start);
    }

    public int getStartColumn() {
        return IntPair.getSecond(start);
    }

    public int getEndLine() {
        return IntPair.getFirst(end);
    }

    public int getEndColumn() {
        return IntPair.getSecond(end);
    }

    public FoldingRegion createChild(int startLine, int startColumn, int endLine, int endColumn) {
        if (startLine < getStartLine() || (startLine == getStartLine() && startColumn < getStartColumn())) {
            throw new IllegalArgumentException("child start is before parent start");
        }
        if (endLine > getEndLine() || (endLine == getEndLine() && endColumn > getEndColumn())) {
            throw new IllegalArgumentException("child end is beyond parent end");
        }
        var child = new FoldingRegion(startLine, startColumn, endLine, endColumn);
        children.add(child);
        return child;
    }

}

