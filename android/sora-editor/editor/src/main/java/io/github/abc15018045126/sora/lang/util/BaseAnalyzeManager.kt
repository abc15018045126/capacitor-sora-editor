package io.github.abc15018045126.sora.lang.util

import android.os.Bundle
import androidx.annotation.CallSuper
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager
import io.github.abc15018045126.sora.lang.analysis.StyleReceiver
import io.github.abc15018045126.sora.text.ContentReference


abstract class BaseAnalyzeManager : AnalyzeManager {

    override var receiver: StyleReceiver? = null


    var extraArguments: Bundle? = null
        private set


    var contentRef: ContentReference? = null
        private set

    @CallSuper
    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.extraArguments = extraArguments
        this.contentRef = content
        rerun()
    }

    @CallSuper
    override fun destroy() {
        this.receiver = null
        this.contentRef = null
        this.extraArguments = null
    }

}
