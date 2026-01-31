
package io.github.abc15018045126.sora.lang.completion.snippet.parser;

public class Token {

    public Token(int index, int length, TokenType type) {
        this.index = index;
        this.length = length;
        this.type = type;
    }

    public int index;
    public int length;
    public TokenType type;

    @Override
    public String toString() {
        return "Token{" +
                "index=" + index +
                ", length=" + length +
                ", type=" + type +
                '}';
    }
}

