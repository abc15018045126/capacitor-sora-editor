
package io.github.abc15018045126.sora.event;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.styling.Span;
import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.TextRange;
import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Report a single click
 *
 * @author abc15018045126
 */
public class ClickEvent extends EditorMotionEvent {

    public ClickEvent(@NonNull CodeEditor editor, @NonNull CharPosition position, @NonNull MotionEvent event,
                      @Nullable Span span, @Nullable TextRange spanRange, int motionRegion, int motionBound) {
        super(editor, position, event, span, spanRange, motionRegion, motionBound);
    }

}

