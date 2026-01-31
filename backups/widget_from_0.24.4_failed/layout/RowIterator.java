
package io.github.abc15018045126.sora.widget.layout;

import androidx.annotation.NonNull;

import java.util.NoSuchElementException;

/**
 * Row iterator.
 * This iterator is able to return a series of Row objects linearly
 * Editor uses this to get information of rows and paint them accordingly
 *
 * @author Rose
 */
public interface RowIterator {

    /**
     * Return next Row object
     * <p>
     * The result should not be stored, because implementing classes will always return the same
     * object due to performance
     *
     * @return Row object contains the information about a row
     * @throws NoSuchElementException If no more row available
     */
    @NonNull
    Row next();

    /**
     * Whether there is more Row object
     *
     * @return Whether more row available
     */
    boolean hasNext();

    /**
     * Reset the position to its original position.
     * <p>
     * This can be useful when the elements should be iterated for
     * several times.
     */
    void reset();

}
