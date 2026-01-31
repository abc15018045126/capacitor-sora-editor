
package io.github.abc15018045126.sora.text;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Line separator types
 *
 * @author abc15018045126
 */
public enum LineSeparator {

    /**
     * No separator. Used internally
     */
    NONE(""),
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    private final String str;
    private final int length;
    private final char[] chars;

    LineSeparator(String str) {
        this.str = str;
        this.length = str.length();
        chars = str.toCharArray();
    }

    /**
     * Get the text of this separator
     */
    public String getContent() {
        return str;
    }

    /**
     * Get text length of this separator
     */
    public int getLength() {
        return length;
    }

    /**
     * Get a char array containing the line separator. The char array should not be modified.
     */
    public char[] getChars() {
        return chars;
    }

    /**
     * Get target line separator from a line separator string.
     *
     * @param str line separator string
     * @throws IllegalArgumentException if the given str is not a line separator
     */
    public static LineSeparator fromSeparatorString(String str) {
        Objects.requireNonNull(str, "text must not be null");
        switch (str) {
            case "\r":
                return CR;
            case "\n":
                return LF;
            case "\r\n":
                return CRLF;
            case "":
                return NONE;
            default:
                throw new IllegalArgumentException("unknown line separator type");
        }
    }

    /**
     * Get target line separator from a line separator string.
     *
     * @param text  the whole text
     * @param start start index of the line separator
     * @param end   end index of the line separator
     * @throws IllegalArgumentException if the given str is not a line separator
     */
    public static LineSeparator fromSeparatorString(@NonNull CharSequence text, int start, int end) {
        Objects.requireNonNull(text, "text must not be null");
        if (end == start) {
            return NONE;
        }
        if (end - start == 1) {
            var ch = text.charAt(start);
            if (ch == '\r') return CR;
            if (ch == '\n') return LF;
        }
        if (end - start == 2 && text.charAt(start) == '\r' && text.charAt(start + 1) == '\n') {
            return CRLF;
        }
        throw new IllegalArgumentException("unknown line separator type");
    }

}

