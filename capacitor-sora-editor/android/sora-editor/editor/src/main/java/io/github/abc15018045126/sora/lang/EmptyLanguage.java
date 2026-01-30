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
package io.github.abc15018045126.sora.lang;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager;
import io.github.abc15018045126.sora.lang.completion.CompletionPublisher;
import io.github.abc15018045126.sora.lang.format.Formatter;
import io.github.abc15018045126.sora.lang.smartEnter.NewlineHandler;
import io.github.abc15018045126.sora.lang.util.BaseAnalyzeManager;
import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.text.ContentReference;
import io.github.abc15018045126.sora.text.TextRange;
import io.github.abc15018045126.sora.widget.SymbolPairMatch;

/**
 * Empty language
 *
 * @author abc15018045126
 */
public class EmptyLanguage implements Language {


    public final static SymbolPairMatch EMPTY_SYMBOL_PAIRS = new SymbolPairMatch();

    @NonNull
    @Override
    public Formatter getFormatter() {
        return EmptyFormatter.INSTANCE;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        return EMPTY_SYMBOL_PAIRS;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) {

    }

    @Override
    public int getInterruptionLevel() {
        return INTERRUPTION_LEVEL_STRONG;
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return new NewlineHandler[0];
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return EmptyAnalyzeManager.INSTANCE;
    }

    @Override
    public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
        return 0;
    }

    @Nullable
    @Override
    public QuickQuoteHandler getQuickQuoteHandler() {
        return null;
    }


    @Override
    public void destroy() {

    }

    @Override
    public boolean useTab() {
        return false;
    }

    public static class EmptyFormatter implements Formatter {

        public final static EmptyFormatter INSTANCE = new EmptyFormatter();

        @Override
        public void format(@NonNull Content text, @NonNull TextRange cursorRange) {

        }

        @Override
        public void formatRegion(@NonNull Content text, @NonNull TextRange rangeToFormat, @NonNull TextRange cursorRange) {

        }

        @Override
        public void setReceiver(@Nullable FormatResultReceiver receiver) {

        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void destroy() {

        }
    }

    public static class EmptyAnalyzeManager extends BaseAnalyzeManager {

        public final static EmptyAnalyzeManager INSTANCE = new EmptyAnalyzeManager();


        @Override
        public void insert(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence insertedContent) {

        }

        @Override
        public void delete(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence deletedContent) {

        }

        @Override
        public void rerun() {

        }

    }
}


