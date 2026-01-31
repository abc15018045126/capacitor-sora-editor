
package io.github.abc15018045126.sora.text.method;

import android.text.Editable;
import android.view.KeyEvent;

import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Handles key events such as SHIFT
 *
 * @author abc15018045126
 */
public class KeyMetaStates extends android.text.method.MetaKeyKeyListener {

    private final CodeEditor editor;

    /**
     * Dummy text used for Android original APIs
     */
    private final Editable dest = Editable.Factory.getInstance().newEditable("");
    private boolean isCtrlPressed = false;

    public KeyMetaStates(CodeEditor editor) {
        this.editor = editor;
    }

    public void onKeyDown(KeyEvent event) {
        super.onKeyDown(editor, dest, event.getKeyCode(), event);
        isCtrlPressed = event.isCtrlPressed();
    }

    public void onKeyUp(KeyEvent event) {
        super.onKeyUp(editor, dest, event.getKeyCode(), event);
        isCtrlPressed = event.isCtrlPressed();
    }

    public int getMetaState(KeyEvent event) {
        return getMetaState(dest, event);
    }

    public boolean isCtrlPressed() {
        return isCtrlPressed;
    }

    public boolean isShiftPressed() {
        return getMetaState(dest, META_SHIFT_ON) == 1;
    }

    public boolean isAltPressed() {
        return getMetaState(dest, META_ALT_ON) == 1;
    }

    public boolean isSymPressed() {
        return getMetaState(dest, META_SYM_ON) == 1;
    }

    public boolean isSelecting() {
        return isShiftPressed() && !isAltPressed();
    }

    public void adjustAfterKeyPress() {
        adjustMetaAfterKeypress(dest);
    }

    public void clearMetaStates(int states) {
        clearMetaKeyState(editor, dest, states);
    }

}

