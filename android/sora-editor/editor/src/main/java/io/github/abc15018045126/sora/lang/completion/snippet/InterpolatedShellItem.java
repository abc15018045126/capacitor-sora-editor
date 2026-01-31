
package io.github.abc15018045126.sora.lang.completion.snippet;

import androidx.annotation.NonNull;

public class InterpolatedShellItem extends SnippetItem {

    private String shellCode;

    public InterpolatedShellItem(@NonNull String shellCode, int index) {
        super(index);
        this.shellCode = shellCode;
    }

    @NonNull
    public String getShellCode() {
        return shellCode;
    }

    public void setShellCode(@NonNull String shellCode) {
        this.shellCode = shellCode;
    }

    @NonNull
    @Override
    public InterpolatedShellItem clone() {
        var n = new InterpolatedShellItem(shellCode, getStartIndex());
        n.setIndex(getStartIndex(), getEndIndex());
        return n;
    }

}

