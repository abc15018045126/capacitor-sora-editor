
package io.github.abc15018045126.sora.lang.completion.snippet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VariableItem extends SnippetItem implements PlaceHolderElement {

    private String name;
    private String defaultValue;
    private Transform transform;

    public VariableItem(int index, @NonNull String name, @Nullable String defaultValue) {
        this(index, name, defaultValue, null);
    }

    public VariableItem(int index, @NonNull String name, @Nullable String defaultValue, @Nullable Transform transform) {
        super(index);
        this.name = name;
        this.defaultValue = defaultValue;
        this.transform = transform;
    }

    public void setTransform(@Nullable Transform transform) {
        this.transform = transform;
    }

    @Nullable
    public Transform getTransform() {
        return transform;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setDefaultValue(@NonNull String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Nullable
    public String getDefaultValue() {
        return defaultValue;
    }

    @NonNull
    @Override
    public VariableItem clone() {
        var n = new VariableItem(getStartIndex(), name, defaultValue);
        n.setIndex(getStartIndex(), getEndIndex());
        return n;
    }
}

