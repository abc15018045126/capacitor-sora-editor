
package io.github.abc15018045126.sora.langs.textmate;

import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;

import java.util.List;

public class MyState {

    public MyState(IStateStack tokenizeState, OnigResult foldingCache, int indent, List<String> identifiers) {
        this.tokenizeState = tokenizeState;
        this.foldingCache = foldingCache;
        this.indent = indent;
        this.identifiers = identifiers;
    }

    public IStateStack tokenizeState;
    public OnigResult foldingCache;
    public List<String> identifiers;
    public int indent;

}

