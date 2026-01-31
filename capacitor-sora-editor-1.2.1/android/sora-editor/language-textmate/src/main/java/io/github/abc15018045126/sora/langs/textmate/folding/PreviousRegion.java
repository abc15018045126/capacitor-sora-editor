
package io.github.abc15018045126.sora.langs.textmate.folding;

public class PreviousRegion {
    // indent or -2 if a marker
    public int indent;
    // end line number for the region above
    public int endAbove;
    // start line of the region. Only used for marker regions.
    public int line;

    public PreviousRegion(int indent, int endAbove, int line) {
        this.indent = indent;
        this.endAbove = endAbove;
        this.line = line;
    }

}

