package io.github.abc15018045126.sora.util


class LongArrayList {

    private var data: LongArray = LongArray(64)
    private var length: Int = 0


    fun add(value: Long) {
        data[length++] = value
        if (data.size == length) {
            val newData = LongArray(length shl 1)
            System.arraycopy(data, 0, newData, 0, length)
            data = newData
        }
    }


    val size: Int
        get() = length

    fun size(): Int = length


    fun set(index: Int, value: Long) {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        data[index] = value
    }


    fun lowerBoundByFirst(key: Int): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = IntPair.getFirst(data[mid])

            if (midVal < key)
                low = mid + 1
            else if (midVal > key)
                high = mid - 1
            else
                return mid
        }
        return low
    }

    fun lowerBound(key: Long): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = data[mid]

            if (midVal < key)
                low = mid + 1
            else if (midVal > key)
                high = mid - 1
            else
                return mid
        }
        return low
    }


    fun get(index: Int): Long {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        return data[index]
    }

    fun clear() {
        length = 0
    }
}
