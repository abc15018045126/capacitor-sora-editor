
package io.github.abc15018045126.sora.lang.completion.snippet;

import java.util.List;
import java.util.regex.Pattern;

public class Transform {

    public Pattern getRegexp() {
        return regexp;
    }

    public void setRegexp(Pattern regexp) {
        this.regexp = regexp;
    }

    public List<FormatString> getFormat() {
        return format;
    }

    public void setFormat(List<FormatString> format) {
        this.format = format;
    }

    public Pattern regexp;

    public boolean globalMode;

    public List<FormatString> format;

}

