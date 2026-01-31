
package io.github.abc15018045126.sora.text;

import android.text.GetChars;

import androidx.annotation.NonNull;

import java.nio.CharBuffer;

/**
 * Wrapper for char array. Make char array work as a char sequence.
 *
 * @author abc15018045126
 */
public class CharArrayWrapper implements CharSequence, GetChars {

    private final char[] data;
    private final int offset;
    private int count;

    public CharArrayWrapper(@NonNull char[] array, int dataCount) {
        this(array, 0, dataCount);
    }

    public CharArrayWrapper(@NonNull char[] array, int startOffset, int dataCount) {
        data = array;
        count = dataCount;
        offset = startOffset;
    }

    public void setDataCount(int count) {
        this.count = count;
    }

    @Override
    public int length() {
        return count;
    }

    @Override
    public char charAt(int index) {
        return data[offset + index];
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return CharBuffer.wrap(data, offset + start, end - start);
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destOffset) {
        if (end > count) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(data, start + this.offset, dest, destOffset, end - start);
    }
}

