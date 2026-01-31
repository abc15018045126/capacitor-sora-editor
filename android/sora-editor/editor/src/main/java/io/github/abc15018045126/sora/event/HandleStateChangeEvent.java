
package io.github.abc15018045126.sora.event;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Notifies a selection handle's touch state has changed
 *
 * @author abc15018045126
 */
public class HandleStateChangeEvent extends Event {

    public final static int HANDLE_TYPE_INSERT = 0;
    public final static int HANDLE_TYPE_LEFT = 1;
    public final static int HANDLE_TYPE_RIGHT = 2;
    private final int which;
    private final boolean isHeld;

    public HandleStateChangeEvent(@NonNull CodeEditor editor, int which, boolean heldState) {
        super(editor);
        this.which = which;
        isHeld = heldState;
    }

    /**
     * Get handle type of this event
     * @see #HANDLE_TYPE_LEFT
     * @see #HANDLE_TYPE_RIGHT
     * @see #HANDLE_TYPE_INSERT
     */
    public int getHandleType() {
        return which;
    }

    /**
     * Is the handle held now
     */
    public boolean isHeld() {
        return isHeld;
    }

}

