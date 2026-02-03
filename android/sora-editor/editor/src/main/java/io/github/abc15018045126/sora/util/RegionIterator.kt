package io.github.abc15018045126.sora.util

import kotlin.math.min


open class RegionIterator(

    val max: Int,
    vararg providers: RegionProvider
) {

    private val providers: Array<out RegionProvider> = providers
    private val pointers: IntArray = IntArray(providers.size)
    private val pointerStates: BooleanArray = BooleanArray(providers.size)
    protected var start: Int = 0
    protected var end: Int = 0


    fun nextRegion() {
        start = end
        var minNext = max
        for (i in providers.indices) {
            var next = max
            if (pointers[i] < providers[i].pointCount) {
                val value = providers[i].getPointAt(pointers[i])
                if (value <= max) {
                    next = value
                }
            }
            minNext = min(next, minNext)
        }
        end = minNext
        for (i in providers.indices) {
            if (pointers[i] < providers[i].pointCount && providers[i].getPointAt(pointers[i]) == minNext) {
                pointers[i]++
                pointerStates[i] = true
            } else {
                pointerStates[i] = false
            }
        }
    }


    fun hasNextRegion(): Boolean {
        return end < max
    }


    fun getPointer(i: Int): Int {
        return pointers[i]
    }


    fun getRegionSourcePointer(i: Int): Int {
        val pointerValue = if (pointers[i] < providers[i].pointCount) providers[i].getPointAt(i) else max
        return if (end <= pointerValue && pointerValue < max || pointerStates[i]) pointers[i] - 1 else pointers[i]
    }

    fun getPointerValue(i: Int, j: Int): Int {
        val provider = providers[i]
        if (j < 0) {
            return 0
        }
        if (j >= provider.pointCount) {
            return max
        }
        val value = provider.getPointAt(j)
        return min(value, max)
    }


    fun getStartIndex(): Int {
        return start
    }


    fun getEndIndex(): Int {
        return min(end, max)
    }


    interface RegionProvider {


        val pointCount: Int


        fun getPointAt(index: Int): Int
    }
}
