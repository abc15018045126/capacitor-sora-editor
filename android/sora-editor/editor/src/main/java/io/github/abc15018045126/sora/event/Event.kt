package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor


abstract class Event @JvmOverloads constructor(
    val editor: CodeEditor,
    open val eventTime: Long = System.currentTimeMillis()
) {
    var interceptTargets: Int = 0
        private set


    open fun canIntercept(): Boolean {
        return false
    }


    fun intercept() {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = InterceptTarget.TARGET_EDITOR or InterceptTarget.TARGET_RECEIVERS
    }


    fun intercept(targets: Int) {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = targets
    }


    fun isIntercepted(): Boolean {
        return interceptTargets != 0
    }
}
