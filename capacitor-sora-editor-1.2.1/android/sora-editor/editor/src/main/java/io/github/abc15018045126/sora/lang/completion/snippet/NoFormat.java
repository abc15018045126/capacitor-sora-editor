
package io.github.abc15018045126.sora.lang.completion.snippet;

public class NoFormat implements FormatString {

    private String text;

    public NoFormat(String text) {
        setText(text);
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}

