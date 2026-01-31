
package io.github.abc15018045126.sora.langs.textmate.registry.model;


import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.registry.IGrammarSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface GrammarDefinition {

    String getName();

    @Nullable
    String getLanguageConfiguration();

    @Nullable
    String getScopeName();

    default Map<String,String> getEmbeddedLanguages() {
        return Collections.emptyMap();
    }

    IGrammarSource getGrammar();

}

