

package io.github.abc15018045126.sora.util

class ReversedListView<E>(private val src: List<E>) : AbstractList<E>() {

    override val size: Int
        get() = src.size

    override fun get(index: Int): E {
        return src[src.size - 1 - index]
    }

}
