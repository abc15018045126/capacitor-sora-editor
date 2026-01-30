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

import io.github.abc15018045126.sora.text.ContentLine;

public class WordBreakerProgram extends WordBreakerIcu {

    public WordBreakerProgram(@NonNull ContentLine text) {
        super(text);
    }

    @Override
    public int getOptimizedBreakPoint(int start, int end) {
        int icuResult = super.getOptimizedBreakPoint(start, end);
        if (icuResult != end || end <= start || /* end > start */ Character.isWhitespace(chars[end - 1])) {
            return icuResult;
        }
        // Add extra opportunities for dots
        int index = end - 1;
        while (index > start) {
            if (chars[index] == '.' && index - 1 >= start && !Character.isDigit(chars[index - 1])) {
                // Break after this dot
                return index + 1;
            }
            index--;
        }
        return end;
    }
}

