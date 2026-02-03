

package io.github.abc15018045126.sora.lang.analysis


interface StyleUpdateRange {


    fun isInRange(line: Int): Boolean


    fun lineIndexIterator(maxLineIndex: Int): IntIterator

}
