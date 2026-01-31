
package io.github.abc15018045126.sora.langs.textmate.registry.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.registry.IGrammarSource;

import java.util.Collections;
import java.util.Map;

import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils;

public class DefaultGrammarDefinition implements GrammarDefinition {

    private String name;

    private String languageConfigurationPath;

    private IGrammarSource grammarSource;

    private String scopeName = null;

    private Map<String, String> embeddedLanguages = null;

    private DefaultGrammarDefinition(String name, String scopeName,
                                     IGrammarSource grammarSource, String languageConfigurationPath) {

        this.name = name;
        this.scopeName = scopeName;
        this.grammarSource = grammarSource;
        this.languageConfigurationPath = languageConfigurationPath;
    }


    private DefaultGrammarDefinition(String name, String scopeName,
                                     IGrammarSource grammarSource, String languageConfigurationPath, Map<String, String> embeddedLanguages) {

        this(name, scopeName, grammarSource, languageConfigurationPath);
        this.embeddedLanguages = embeddedLanguages;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getLanguageConfiguration() {
        return languageConfigurationPath;
    }

    @Override
    public @Nullable String getScopeName() {
        return scopeName;
    }

    @Override
    public IGrammarSource getGrammar() {
        return grammarSource;
    }

    @Override
    public Map<String, String> getEmbeddedLanguages() {
        return embeddedLanguages == null ? Collections.emptyMap() : embeddedLanguages;
    }


    public GrammarDefinition withEmbeddedLanguages(Map<String, String> embeddedLanguages) {
        if (embeddedLanguages == null) {
            return this;
        }
        return new DefaultGrammarDefinition(this.name, this.scopeName,
                this.grammarSource, this.languageConfigurationPath,
                embeddedLanguages);
    }

    public static DefaultGrammarDefinition withGrammarSource(IGrammarSource grammarSource) {
        var languageNameByPath = StringUtils.getFileNameWithoutExtension(grammarSource.getFilePath());
        return withGrammarSource(grammarSource, languageNameByPath, "source." + languageNameByPath);
    }

    public static DefaultGrammarDefinition withLanguageConfiguration(IGrammarSource grammarSource, String languageConfigurationPath) {
        var languageNameByPath = StringUtils.getFileNameWithoutExtension(grammarSource.getFilePath());
        return withLanguageConfiguration(grammarSource, languageConfigurationPath, languageNameByPath, "source." + languageNameByPath);
    }


    public static DefaultGrammarDefinition withLanguageConfiguration(IGrammarSource grammarSource, String languageConfigurationPath, String languageName, String scopeName) {
        return new DefaultGrammarDefinition(languageName, scopeName, grammarSource, languageConfigurationPath);
    }


    public static DefaultGrammarDefinition withGrammarSource(IGrammarSource grammarSource, String languageName, String scopeName) {
        return new DefaultGrammarDefinition(languageName, scopeName, grammarSource, null);
    }


}

