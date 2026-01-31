
package io.github.abc15018045126.sora.lang.completion.snippet;

public class ConditionalFormat implements FormatString {

    public int group;

    public String ifValue;

    public String elseValue;

    public String shorthand;

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public String getIfValue() {
        return ifValue;
    }

    public void setIfValue(String ifValue) {
        this.ifValue = ifValue;
    }

    public String getElseValue() {
        return elseValue;
    }

    public void setElseValue(String elseValue) {
        this.elseValue = elseValue;
    }

    public void setShorthand(String shorthand) {
        this.shorthand = shorthand;
    }

    public String getShorthand() {
        return shorthand;
    }
}

