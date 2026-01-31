
package io.github.abc15018045126.sora.event;

import android.view.KeyEvent;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Keybinding event in editor.
 *
 * <p> This is different from {@link EditorKeyEvent}.
 * An {@code EditorKeyEvent} is dispatched by the editor whenever there is a key event.
 * However, a {@code KeyBindingEvent} is dispatched only for keybindings i.e.
 * when multiple keys are pressed at once.
 * For example, <b>Ctrl + X, Ctrl + D, Ctrl + Alt + O, etc.</b>
 * </p>
 *
 * <p>
 * This event is dispatched <strong>after</strong> the {@link EditorKeyEvent}.
 * So, if any {@code EditorKeyEvent} consumes the event (sets the {@link InterceptTarget#TARGET_EDITOR} flag),
 * {@code KeyBindingEvent} will not be called.
 * </p>
 *
 * @author Akash Yadav
 */
public class KeyBindingEvent extends EditorKeyEvent {

    private final boolean editorAbleToHandle;

    /**
     * Creates a new {@code KeyBindingEvent} instance.
     *
     * @param editor             The editor.
     * @param src                The source {@link KeyEvent}.
     * @param type               The key event type.
     * @param editorAbleToHandle <code>true</code> if the editor can handle this event, <code>false</code> otherwise.
     */
    public KeyBindingEvent(@NonNull CodeEditor editor, @NonNull KeyEvent src, Type type, boolean editorAbleToHandle) {
        super(editor, src, type);
        this.editorAbleToHandle = editorAbleToHandle;
    }

    /**
     * Is the editor capable of handling this key binding event?
     *
     * @return <code>true</code> if the editor can handle this event. <code>false</code> otherwise.
     */
    public boolean canEditorHandle() {
        return this.editorAbleToHandle;
    }

}

