
package io.github.abc15018045126.sora.graphics;

import io.github.abc15018045126.sora.util.MyCharacter;

public class GraphicCharacter {

    public static boolean isCombiningCharacter(int codePoint) {
        return MyCharacter.isVariationSelector(codePoint) || MyCharacter.isFitzpatrick(codePoint)
                || MyCharacter.isZWJ(codePoint) || MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint)
                || (Character.charCount(codePoint) == 1 && Character.isSurrogate((char) codePoint))
                || isASCIICombiningSymbol(codePoint);
    }

    public static boolean isASCIICombiningSymbol(int codePoint) {
        return codePoint == '.' || codePoint == '/' || codePoint == '!' || codePoint == '=' ||
                codePoint == '<' || codePoint == '>' || codePoint == '-';/*!(codePoint >= '0' && codePoint <= '9')
                && !(codePoint >= 'a' && codePoint <= 'z')
                && !(codePoint >= 'A' && codePoint <= 'Z');*/
    }

    public static boolean couldBeEmojiPart(int codePoint) {
        return MyCharacter.isVariationSelector(codePoint) || MyCharacter.isFitzpatrick(codePoint)
                || MyCharacter.isZWJ(codePoint) || MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint);
    }

}

