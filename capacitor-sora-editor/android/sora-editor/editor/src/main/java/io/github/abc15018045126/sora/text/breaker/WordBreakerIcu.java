/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/abc15018045126/sora-editor
 *    Copyright (C) 2020-2025  abc15018045126
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact abc15018045126 by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.abc15018045126.sora.text.breaker;

import androidx.annotation.NonNull;

import java.text.BreakIterator;

import io.github.abc15018045126.sora.text.CharSequenceIterator;
import io.github.abc15018045126.sora.text.ContentLine;

public class WordBreakerIcu implements WordBreaker {

    protected final BreakIterator wrappingIterator;

    protected final char[] chars;

    public WordBreakerIcu(@NonNull ContentLine text) {
        this.chars = text.getBackingCharArray();
        var textIterator = new CharSequenceIterator(text);
        wrappingIterator = BreakIterator.getLineInstance();
        wrappingIterator.setText(textIterator);
    }

    public int getOptimizedBreakPoint(int start, int end) {
        // Merging trailing whitespaces is not supported by editor, so force to break here
        if (end > 0 && !Character.isWhitespace(chars[end - 1]) && !wrappingIterator.isBoundary(end)) {
            // Break text at last boundary
            int lastBoundary = wrappingIterator.preceding(end);
            if (lastBoundary != BreakIterator.DONE) {
                int suggestedNext = Math.max(start, Math.min(end, lastBoundary));
                if (suggestedNext > start) {
                    end = suggestedNext;
                }
            }
        }
        return end;
    }

}

