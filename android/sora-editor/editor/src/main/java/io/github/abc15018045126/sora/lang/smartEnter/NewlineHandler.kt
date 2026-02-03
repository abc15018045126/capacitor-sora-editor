package io.github.abc15018045126.sora.lang.smartEnter

import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content


interface NewlineHandler {


    fun matchesRequirement(text: Content, position: CharPosition, style: Styles?): Boolean


    fun handleNewline(text: Content, position: CharPosition, style: Styles?, tabSize: Int): NewlineHandleResult

}
