
package io.github.abc15018045126.sora.event;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Notify that snippet controller state is changed.
 * <br/>
 * If action is {@link #ACTION_START} and any event receiver intercepts editor, the snippet edit will
 * stop before moving to any tab stop. And consequently, a {@link SnippetEvent} with action {@link #ACTION_STOP}
 * will be broadcast immediately.
 * <br/>
 * There is at least one tab stop in the list when action is {@link #ACTION_START} or {@link #ACTION_SHIFT}.
 * But no tab stop is left there when action is {@link #ACTION_STOP}. The last tab stop is where the selection
 * will be placed when the snippet is finished normally.
 *
 * @author abc15018045126
 */
public class SnippetEvent extends Event {

    /**
     * Called before controller shifts to any tab stop
     */
    public final static int ACTION_START = 1;
    /**
     * Called when controller shifted to a tab stop
     */
    public final static int ACTION_SHIFT = 2;
    /**
     * Called when controller <strong>has exited</strong> a snippet
     */
    public final static int ACTION_STOP = 3;

    private final int action;
    private final int currentTabStop;
    private final int totalTabStop;

    public SnippetEvent(@NonNull CodeEditor editor, int action, int currentTabStop, int totalTabStop) {
        super(editor);
        this.action = action;
        this.currentTabStop = currentTabStop;
        this.totalTabStop = totalTabStop;
    }

    /**
     * @see #ACTION_START
     * @see #ACTION_SHIFT
     * @see #ACTION_STOP
     */
    public int getAction() {
        return action;
    }

    /**
     * Get the current index of tab stops
     */
    public int getCurrentTabStop() {
        return currentTabStop;
    }

    /**
     * Get the count of tab stops
     */
    public int getTotalTabStop() {
        return totalTabStop;
    }
}

