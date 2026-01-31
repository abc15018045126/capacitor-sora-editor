
package io.github.abc15018045126.sora.text;

import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage;

@UnsupportedUserUsage
public class ComposingText {

    public int startIndex, endIndex;
    public boolean preSetComposing;

    public void set(int start, int end) {
        this.startIndex = start;
        this.endIndex = end;
    }

    public void adjustLength(int length) {
        this.endIndex = startIndex + length;
    }

    public void reset() {
        this.startIndex = this.endIndex = -1;
        preSetComposing = false;
    }

    public boolean isComposing() {
        return preSetComposing || startIndex >= 0 && endIndex >= 0;
    }

    public void shiftOnInsert(int insertStart, int insertEnd) {
        var length = insertEnd - insertStart;
        if (startIndex <= insertStart && endIndex >= insertStart) {
            endIndex += length;
        }
        // Type 2, text is inserted before a diagnostic
        if (startIndex > insertStart) {
            startIndex += length;
            endIndex += length;
        }
    }

    public void shiftOnDelete(int deleteStart, int deleteEnd) {
        var length = deleteEnd - deleteStart;
        // Compute cross length
        var sharedStart = Math.max(deleteStart, startIndex);
        var sharedEnd = Math.min(deleteEnd, endIndex);
        if (sharedEnd <= sharedStart) {
            // No shared region
            if (startIndex >= deleteEnd) {
                // Shift left
                startIndex -= length;
                endIndex -= length;
            }
        } else {
            // Has shared region
            var sharedLength = sharedEnd - sharedStart;
            endIndex -= sharedLength;
            if (startIndex > deleteStart) {
                // Shift left
                var shiftLeftCount = startIndex - deleteStart;
                startIndex -= shiftLeftCount;
                endIndex -= shiftLeftCount;
            }
        }
    }


}

