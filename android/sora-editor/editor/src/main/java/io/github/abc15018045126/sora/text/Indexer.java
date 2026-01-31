
package io.github.abc15018045126.sora.text;

import androidx.annotation.NonNull;

/**
 * A helper class for ITextContent to transform (line,column) and index
 *
 * @author Rose
 */
public interface Indexer {

    /**
     * Get the index of (line,column)
     *
     * @param line   The line position of index
     * @param column The column position of index
     * @return Calculated index
     */
    int getCharIndex(int line, int column);

    /**
     * Get the line position of index
     *
     * @param index The index you want to know its line
     * @return Line position of index
     */
    int getCharLine(int index);

    /**
     * Get the column position of index
     *
     * @param index The index you want to know its column
     * @return Column position of index
     */
    int getCharColumn(int index);

    /**
     * Get the CharPosition for the given index
     *
     * @param index The index you want to get
     * @return The CharPosition object.
     */
    @NonNull
    CharPosition getCharPosition(int index);

    /**
     * Get the CharPosition for the given (line,column)
     *
     * @param line   The line position you want to get
     * @param column The column position you want to get
     * @return The CharPosition object.
     */
    @NonNull
    CharPosition getCharPosition(int line, int column);

    /**
     * @param dest Destination of result
     * @see #getCharPosition(int)
     */
    void getCharPosition(int index, @NonNull CharPosition dest);

    /**
     * @param dest Destination of result
     * @see #getCharPosition(int, int)
     */
    void getCharPosition(int line, int column, @NonNull CharPosition dest);

}

