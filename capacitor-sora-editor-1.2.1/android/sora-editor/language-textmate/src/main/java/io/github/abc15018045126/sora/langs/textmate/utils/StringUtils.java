
package io.github.abc15018045126.sora.langs.textmate.utils;

import java.util.regex.Pattern;

public class StringUtils {

    public static boolean checkSurrogate(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isSurrogate(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static int convertUnicodeOffsetToUtf16(String text, int offset, boolean hasSurrogate) {
        if (hasSurrogate) {
            var j = 0;
            for (int i = 0; i < text.length(); i++) {
                if (j == offset) {
                    return i;
                }
                var ch = text.charAt(i);
                if (Character.isHighSurrogate(ch) && i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    i++;
                }
                j++;
            }
        }
        return offset;
    }


    private static final Pattern MATCH_PATTERN = Pattern.compile(".*/|\\..*");

    public static String getFileNameWithoutExtension(String filePath) {
        return MATCH_PATTERN.matcher(filePath).replaceAll("");

    }
}

