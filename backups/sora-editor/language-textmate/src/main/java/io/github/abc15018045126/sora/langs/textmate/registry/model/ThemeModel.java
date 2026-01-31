
package io.github.abc15018045126.sora.langs.textmate.registry.model;

import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme;
import org.eclipse.tm4e.core.internal.theme.raw.RawTheme;
import org.eclipse.tm4e.core.internal.theme.raw.RawThemeReader;
import org.eclipse.tm4e.core.registry.IThemeSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils;

public class ThemeModel {

    public static final ThemeModel EMPTY = new ThemeModel("EMPTY");
    private IThemeSource themeSource;

    private IRawTheme rawTheme;

    private Theme theme;

    private String name;

    private boolean isDark;

    public ThemeModel(IThemeSource themeSource) {
        this.themeSource = themeSource;
        this.name = StringUtils.getFileNameWithoutExtension(themeSource.getFilePath());
    }

    public ThemeModel(IThemeSource themeSource, String name) {
        this.themeSource = themeSource;
        this.name = name;
    }

    private ThemeModel(String name) {
        themeSource = null;
        rawTheme = null;
        this.name = name;
        theme = Theme.createFromRawTheme(null, null);
    }

    public void setDark(boolean dark) {
        isDark = dark;
    }

    public boolean isDark() {
        return isDark;
    }

    public void load() throws Exception {
        load(null);
    }

    public void load(List<String> colorMap) throws Exception {
        rawTheme = RawThemeReader.readTheme(themeSource);
        theme = Theme.createFromRawTheme(rawTheme, colorMap);
    }

    public boolean isLoaded() {
        return theme != null;
    }

    @Nullable
    public IThemeSource getThemeSource() {
        return themeSource;
    }

    @Nullable
    public IRawTheme getRawTheme() {
        return rawTheme;
    }

    public Theme getTheme() {
        return theme;
    }

    public String getName() {
        return name;
    }
}

