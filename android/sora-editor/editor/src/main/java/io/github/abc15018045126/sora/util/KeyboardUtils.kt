

package io.github.abc15018045126.sora.util

import android.content.Context
import android.content.res.Configuration

import android.inputmethodservice.InputMethodService


object KeyboardUtils {


    fun isHardKeyboardConnected(context: Context?): Boolean {
        if (context == null) return false
        val config = context.resources.configuration
        return (config.keyboard != Configuration.KEYBOARD_NOKEYS
                || config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
    }
}
