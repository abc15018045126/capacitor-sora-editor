
package io.github.abc15018045126.sora.langs.textmate.folding;

import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;

public interface FoldingHelper {

    OnigResult getResultFor(int line);

    int getIndentFor(int line);

}

