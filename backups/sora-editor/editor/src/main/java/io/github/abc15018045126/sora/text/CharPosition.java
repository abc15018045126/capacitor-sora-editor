
package io.github.abc15018045126.sora.text;


import androidx.annotation.NonNull;

import java.util.Objects;

import io.github.abc15018045126.sora.util.IntPair;

/**
 * This a data class of a character position in {@link Content}
 *
 * @author abc15018045126
 */
public final class CharPosition {

    public int index;

    public int line;

    public int column;

    public CharPosition() {
    }

    public CharPosition(int line, int column) {
        this(line, column, -1);
    }

    public CharPosition(int line, int column, int index) {
        this.index = index;
        this.line = line;
        this.column = column;
    }

    /**
     * Get the index
     *
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get column
     *
     * @return column
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get line
     *
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * Make this CharPosition zero and return self
     *
     * @return self
     */
    public CharPosition toBOF() {
        index = line = column = 0;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof CharPosition pos) {
            return pos.column == column &&
                    pos.line == line &&
                    pos.index == index;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, line, column);
    }

    /**
     * Convert {@link CharPosition#line} and {@link CharPosition#column} to a Long number
     * <p>
     * First integer is line and second integer is column
     *
     * @return A Long integer describing the position
     */
    public long toIntPair() {
        return IntPair.pack(line, column);
    }

    /**
     * Make a copy of this CharPosition and return the copy
     *
     * @return New CharPosition including info of this CharPosition
     */
    @NonNull
    public CharPosition fromThis() {
        var pos = new CharPosition();
        pos.set(this);
        return pos;
    }

    /**
     * Set this {@link CharPosition} object's data the same as {@code another}
     */
    public void set(@NonNull CharPosition another) {
        index = another.index;
        line = another.line;
        column = another.column;
    }

    @NonNull
    @Override
    public String toString() {
        return "CharPosition(line = " + line + ",column = " + column + ",index = " + index + ")";
    }

}

