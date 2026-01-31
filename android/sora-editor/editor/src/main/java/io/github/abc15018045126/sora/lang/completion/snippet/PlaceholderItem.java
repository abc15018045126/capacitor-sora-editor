
package io.github.abc15018045126.sora.lang.completion.snippet;

import androidx.annotation.NonNull;

public class PlaceholderItem extends SnippetItem {

    private PlaceholderDefinition definition;
    private String text;

    public PlaceholderItem(@NonNull PlaceholderDefinition definition, int index) {
        setIndex(index, index);
        this.definition = definition;
    }

    private PlaceholderItem(@NonNull PlaceholderDefinition definition, String text, int start, int end) {
        setIndex(start, end);
        this.text = text;
        this.definition = definition;
    }

    public void setDefinition(PlaceholderDefinition definition) {
        this.definition = definition;
    }

    public PlaceholderDefinition getDefinition() {
        return definition;
    }

    @NonNull
    @Override
    public PlaceholderItem clone() {
        return new PlaceholderItem(definition, text, getStartIndex(), getEndIndex());
    }
}

