package io.github.abc15018045126.sora.lang.brackets

import io.github.abc15018045126.sora.text.Content


interface BracketsProvider {


    fun getPairedBracketAt(text: Content, index: Int): PairedBracket?

}
