
package io.github.abc15018045126.sora.widget.layout;

import java.util.List;

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint;

/**
 * This class represents a 'row' in editor.
 * Editor uses this to draw rows
 *
 * @author abc15018045126
 */
public class Row {

    /**
     * The index in lines
     * But not row index
     */
    public int lineIndex;

    /**
     * Whether this row is the first one of a line.
     * Editor will draw line number to left of this row to indicate this
     */
    public boolean isLeadingRow;

    /**
     * Whether this row is the last one of a line.
     * Editor will draw soft-wrap or line-break indicator according to this
     */
    public boolean isTrailingRow;

    /**
     * Start index in target line
     */
    public int startColumn;

    /**
     * End index in target line
     */
    public int endColumn;

    /**
     * Inlay hints on the row
     */
    public List<InlayHint> inlayHints;

    /**
     * Extra translation when rendering
     */
    public float renderTranslateX;

}
