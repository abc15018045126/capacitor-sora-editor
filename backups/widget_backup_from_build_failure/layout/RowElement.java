
package io.github.abc15018045126.sora.widget.layout;

import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint;

/**
 * Element on a row
 *
 * @author abc15018045126
 */
public class RowElement {

    /**
     * Type of element.
     *
     * @see RowElementTypes
     */
    public int type;

    /* Fields for type TEXT */

    /**
     * Start column of text
     */
    public int startColumn;
    /**
     * End column of text
     */
    public int endColumn;
    /**
     * Direction of the text run
     */
    public boolean isRtlText;

    /* Fields for type INLAY_HINT */

    /**
     * The inlay hint
     */
    public InlayHint inlayHint;

    /**
     * The expected column position to display after
     */
    public int displayColumnPosition;

}

