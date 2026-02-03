package io.github.abc15018045126.sora.util

import android.util.SparseIntArray
import io.github.abc15018045126.sora.util.IntPair.getFirst
import io.github.abc15018045126.sora.util.IntPair.getSecond
import io.github.abc15018045126.sora.util.IntPair.pack
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class BinaryHeap {


    @JvmField
    val lock: Lock = ReentrantLock()


    private val idToPosition: SparseIntArray = SparseIntArray()


    private var idAllocator = 1


    private var nodeCount = 0


    private var nodes: LongArray = LongArray(129)

    private fun id(value: Long): Int {
        return getFirst(value)
    }

    private fun data(value: Long): Int {
        return getSecond(value)
    }


    fun clear() {
        nodeCount = 0
        idToPosition.clear()
        idAllocator = 1
    }


    fun ensureCapacity(capacity: Int) {
        var cap = capacity
        cap++
        if (nodes.size < cap) {
            val origin = nodes
            if (nodes.size shl 1 >= cap) {
                nodes = LongArray(nodes.size shl 1)
            } else {
                nodes = LongArray(cap)
            }
            System.arraycopy(origin, 0, nodes, 0, nodeCount + 1)
        }
    }


    fun top(): Int {
        if (nodeCount == 0) {
            return 0
        }
        return data(nodes[1])
    }


    fun getNodeCount(): Int {
        return nodeCount
    }


    private fun heapifyDown(position: Int) {
        var pos = position
        var child = pos * 2
        while (child <= nodeCount) {
            val parentNode = nodes[pos]
            var childNode: Long
            if (child + 1 <= nodeCount && data(nodes[child + 1]) > data(nodes[child])) {
                child = child + 1
            }
            childNode = nodes[child]
            if (data(parentNode) < data(childNode)) {
                idToPosition.put(id(childNode), pos)
                idToPosition.put(id(parentNode), child)
                nodes[child] = parentNode
                nodes[pos] = childNode
                pos = child
            } else {
                break
            }
            child = pos * 2
        }
    }


    private fun heapifyUp(position: Int) {
        var pos = position
        var parent = pos / 2
        while (parent >= 1) {
            val childNode = nodes[pos]
            val parentNode = nodes[parent]
            if (data(childNode) > data(parentNode)) {
                idToPosition.put(id(childNode), parent)
                idToPosition.put(id(parentNode), pos)
                nodes[pos] = parentNode
                nodes[parent] = childNode
                pos = parent
            } else {
                break
            }
            parent = pos / 2
        }
    }


    fun push(value: Int): Int {
        ensureCapacity(nodeCount + 1)
        if (idAllocator == Int.MAX_VALUE) {
            throw IllegalStateException("unable to allocate more id")
        }
        val id = idAllocator++
        nodeCount++
        nodes[nodeCount] = pack(id, value)
        idToPosition.put(id, nodeCount)
        heapifyUp(nodeCount)
        return id
    }


    fun update(id: Int, newValue: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to update with an invalid id")
        }
        val origin = data(nodes[position])
        nodes[position] = pack(id(nodes[position]), newValue)
        if (origin < newValue) {
            heapifyUp(position)
        } else if (origin > newValue) {
            heapifyDown(position)
        }
    }


    fun remove(id: Int) {
        val position = idToPosition.get(id, 0)
        if (position == 0) {
            throw IllegalArgumentException("trying to remove with an invalid id")
        }
        idToPosition.delete(id)

        nodes[position] = nodes[nodeCount]

        nodes[nodeCount--] = 0

        if (position == nodeCount + 1) {
            return
        }
        idToPosition.put(id(nodes[position]), position)
        heapifyUp(position)
        heapifyDown(position)
    }
}
