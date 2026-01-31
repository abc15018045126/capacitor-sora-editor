
package io.github.abc15018045126.sora.event;

import android.view.KeyEvent;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.method.KeyMetaStates;
import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Receives key related events in editor.
 * <p>
 * You may set a boolean for editor to return to the Android KeyEvent framework.
 *
 * @author abc15018045126
 * @see ResultedEvent#setResult(Object)
 * <p>
 * This class mirrors methods of {@link KeyEvent}, but some methods are changed:
 * @see #isAltPressed()
 * @see #isShiftPressed()
 */
public class EditorKeyEvent extends ResultedEvent<Boolean> {

    private final KeyEvent src;
    private final Type type;
    private final boolean shiftPressed;
    private final boolean altPressed;

    public EditorKeyEvent(@NonNull CodeEditor editor, @NonNull KeyEvent src, @NonNull Type type) {
        super(editor);
        this.src = src;
        this.type = type;
        shiftPressed = getEditor().getKeyMetaStates().isShiftPressed();
        altPressed = getEditor().getKeyMetaStates().isAltPressed();
    }

    @Override
    public boolean canIntercept() {
        return true;
    }

    public int getAction() {
        return src.getAction();
    }

    public int getKeyCode() {
        return src.getKeyCode();
    }

    public int getRepeatCount() {
        return src.getRepeatCount();
    }

    public int getMetaState() {
        return src.getMetaState();
    }

    public int getModifiers() {
        return src.getModifiers();
    }

    public long getDownTime() {
        return src.getDownTime();
    }

    /**
     * Get the key event type.
     *
     * @return The key event type.
     */
    @NonNull
    public Type getEventType() {
        return this.type;
    }

    @Override
    public long getEventTime() {
        return src.getEventTime();
    }

    /**
     * editor change: track shift/alt by {@link KeyMetaStates}
     */
    public boolean isShiftPressed() {
        return shiftPressed;
    }

    /**
     * editor change: track shift/alt by {@link KeyMetaStates}
     */
    public boolean isAltPressed() {
        return altPressed;
    }

    public boolean isCtrlPressed() {
        return (src.getMetaState() & KeyEvent.META_CTRL_ON) != 0;
    }

    public void markAsConsumed() {
        interceptAndSetResult(true);
    }

    public final boolean result(boolean editorResult) {
        var res = getResult();
        var userResult = res != null && res;
        if (isIntercepted()) {
            return userResult;
        } else {
            return userResult || editorResult;
        }
    }

    /**
     * The type of {@link EditorKeyEvent}.
     */
    public enum Type {

        /**
         * Used for {@link CodeEditor#onKeyUp(int, KeyEvent)}.
         */
        UP,

        /**
         * Used for {@link CodeEditor#onKeyDown(int, KeyEvent)}.
         */
        DOWN,

        /**
         * Used for {@link CodeEditor#onKeyMultiple(int, int, KeyEvent)}.
         */
        MULTIPLE
    }
}

