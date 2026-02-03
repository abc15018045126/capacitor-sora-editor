package io.github.abc15018045126.sora.lang.format

import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.TextRange


interface Formatter {


    fun format(text: Content, cursorRange: TextRange)


    fun formatRegion(text: Content, rangeToFormat: TextRange, cursorRange: TextRange)


    fun setReceiver(receiver: FormatResultReceiver?)


    fun isRunning(): Boolean


    fun destroy()


    fun cancel() {

    }

    interface FormatResultReceiver {

        fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?)


        fun onFormatFail(throwable: Throwable?)
    }
}
