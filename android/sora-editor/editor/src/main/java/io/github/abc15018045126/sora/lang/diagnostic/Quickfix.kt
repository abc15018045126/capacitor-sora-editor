

package io.github.abc15018045126.sora.lang.diagnostic

import android.content.Context



open class Quickfix(
    private val title: CharSequence?,
    val documentVersion: Long = 0,
    private val fixAction: Runnable? = null
) {

    private var resourceId: Int = 0

    constructor(titleRes: Int, documentVersion: Long = 0, fixAction: Runnable) : this(null, documentVersion, fixAction) {
        resourceId = titleRes
    }

    open fun resolveTitle(context: Context) : CharSequence {
        return title ?: context.getString(resourceId)
    }

    open fun executeQuickfix() {
        fixAction?.run()
    }

}
