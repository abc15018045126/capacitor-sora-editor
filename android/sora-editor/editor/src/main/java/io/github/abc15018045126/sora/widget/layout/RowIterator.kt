package io.github.abc15018045126.sora.widget.layout

import java.util.NoSuchElementException


interface RowIterator {

    fun next(): Row


    fun hasNext(): Boolean


    fun reset()
}
