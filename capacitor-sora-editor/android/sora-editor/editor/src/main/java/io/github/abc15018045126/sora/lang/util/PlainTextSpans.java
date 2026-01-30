/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/abc15018045126/sora-editor
 *    Copyright (C) 2020-2024  abc15018045126
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
package io.github.abc15018045126.sora.lang.util;

import io.github.abc15018045126.sora.lang.styling.EmptyReader;
import io.github.abc15018045126.sora.lang.styling.Spans;
import io.github.abc15018045126.sora.text.CharPosition;

/**
 * {@link Spans} implementation that always returns {@link EmptyReader} for reading spans.
 * Line count is automatically adjusted as content changes.
 *
 * @author abc15018045126
 */
public class PlainTextSpans implements Spans {

    private int lineCount;

    public PlainTextSpans(int lineCount) {
        this.lineCount = lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public void adjustOnInsert(CharPosition start, CharPosition end) {
        lineCount += end.line - start.line;
    }

    @Override
    public void adjustOnDelete(CharPosition start, CharPosition end) {
        lineCount -= end.line - start.line;
    }

    @Override
    public Reader read() {
        return EmptyReader.getInstance();
    }

    @Override
    public boolean supportsModify() {
        return false;
    }

    @Override
    public Modifier modify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }
}

