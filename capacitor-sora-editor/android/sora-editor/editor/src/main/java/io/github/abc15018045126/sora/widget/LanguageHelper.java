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
package io.github.abc15018045126.sora.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.Language;
import io.github.abc15018045126.sora.lang.QuickQuoteHandler;
import io.github.abc15018045126.sora.text.ContentReference;

/**
 * Helper class for better Android API compatibility
 *
 * @author abc15018045126
 */
class LanguageHelper {

    @Nullable
    public static QuickQuoteHandler getQuickQuoteHandler(@NonNull Language language) {
        try {
            return language.getQuickQuoteHandler();
        } catch (AbstractMethodError e) {
            return null;
        }
    }

    public static int getIndentAdvance(
            @NonNull Language language,
            @NonNull ContentReference content,
            int line,
            int column,
            int spaceCountOnLine,
            int tabCountOnLine
    ) {
        try {
            return language.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine);
        } catch (AbstractMethodError e) {
            return language.getIndentAdvance(content, line, column);
        }
    }


}

