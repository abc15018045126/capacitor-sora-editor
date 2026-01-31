
package io.github.abc15018045126.sora.event;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.styling.Span;
import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.TextRange;
import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Report double click in editor.
 * This event can be intercepted.
 *
 * @author abc15018045126
 */
public class DoubleClickEvent extends EditorMotionEvent {

    public DoubleClickEvent(@NonNull CodeEditor editor, @NonNull CharPosition position, @NonNull MotionEvent event,
                            @Nullable Span span, @Nullable TextRange spanRange, int motionRegion, int motionBound) {
        super(editor, position, event, span, spanRange, motionRegion, motionBound);
    }

}

