
package io.github.abc15018045126.sora.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * Event with a result
 *
 * @param <T> Result type
 */
public abstract class ResultedEvent<T> extends Event {

    private T result;

    public ResultedEvent(@NonNull CodeEditor editor) {
        super(editor);
    }

    @Nullable
    public T getResult() {
        return result;
    }

    public void setResult(@Nullable T result) {
        this.result = result;
    }

    public void interceptAndSetResult(@Nullable T result) {
        setResult(result);
        intercept();
    }

    public boolean isResultSet() {
        return result != null;
    }

}

