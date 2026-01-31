
package io.github.abc15018045126.sora.langs.textmate.utils;

import androidx.annotation.NonNull;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

public class MatcherUtils {

    public static String replaceAll(@NonNull CharSequence source, @NonNull Matcher matcher, @NonNull Function<MatchResult, String> replacer) {
        matcher.reset();
        var sb = new StringBuilder();
        int appendPos = 0;
        while (matcher.find()) {
            var result = matcher.toMatchResult();
            var replacement = replacer.apply(result);
            sb.append(source, appendPos, result.start());
            sb.append(replacement);
            appendPos = result.end();
        }
        if (sb.length() == 0) {
            // no match
            return source.toString();
        }
        sb.append(source, appendPos, source.length());
        return sb.toString();
    }

}

