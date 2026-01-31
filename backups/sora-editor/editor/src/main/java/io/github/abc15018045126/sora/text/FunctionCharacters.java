
package io.github.abc15018045126.sora.text;

import androidx.annotation.NonNull;

/**
 * Utility for ASCII function characters
 *
 * @author abc15018045126
 */
public class FunctionCharacters {

    private final static String[] names = {
            "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK",
            "BEL", "BS", "HT", "LF", "VT", "FF", "CR", "SO",
            "SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK",
            "SYN", "ETB", "CAN", "EM", "SUB", "ESC", "FS",
            "GS", "RS", "US", "SP"
    };

    /**
     * Check if the letter is ASCII function character.
     */
    public static boolean isFunctionCharacter(char letter) {
        return letter < 32 || letter == 127;
    }

    /**
     * Check if the letter is ASCII function character, '\t' excluded.
     */
    public static boolean isEditorFunctionChar(char letter) {
        return letter != '\t' && isFunctionCharacter(letter);
    }

    /**
     * Get the name of function character
     */
    @NonNull
    public static String getNameForFunctionCharacter(char letter) {
        if (letter < 32) {
            return names[letter];
        } else if (letter == 127) {
            return "DEL";
        }
        return "UNK";
    }

}

