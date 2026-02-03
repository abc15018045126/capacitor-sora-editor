

package io.github.abc15018045126.sora.util

interface ShareableData<T> : Cloneable {


    fun retain()


    fun release()


    fun isMutable(): Boolean


    fun toMutable(): T

}
