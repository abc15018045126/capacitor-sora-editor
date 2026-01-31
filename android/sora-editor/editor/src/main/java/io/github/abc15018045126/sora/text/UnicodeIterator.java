
package io.github.abc15018045126.sora.text;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.util.ObjectPool;

public class UnicodeIterator {

    private final static ObjectPool<UnicodeIterator> sPool = new ObjectPool<>() {
        @Override
        protected UnicodeIterator allocateNew() {
            return new UnicodeIterator();
        }

        @Override
        protected void onRecycleObject(UnicodeIterator recycledObj) {
            recycledObj.text = null;
        }
    };

    private CharSequence text;
    private int codePoint;
    private int start;
    private int end;
    private int limit;

    private UnicodeIterator() {

    }

    public static UnicodeIterator obtain(@NonNull CharSequence text, int start, int end) {
        var r = sPool.obtain();
        r.set(text, start, end);
        return r;
    }

    public void recycle() {
        sPool.recycle(this);
    }

    public void set(@NonNull CharSequence text, int start, int end) {
        if ((start | end | (end - start) | (text.length() - end)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.text = text;
        this.start = this.end = start;
        limit = end;
    }

    public boolean hasNext() {
        return end < limit;
    }

    public int nextCodePoint() {
        start = end;
        if (start >= limit) {
            codePoint = 0;
        } else {
            end++;
            var ch = text.charAt(start);
            if (Character.isHighSurrogate(ch) && end < limit) {
                codePoint = Character.toCodePoint(ch, text.charAt(end));
                end++;
            } else {
                codePoint = ch;
            }
        }
        return codePoint;
    }

    public int getCodePoint() {
        return codePoint;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
