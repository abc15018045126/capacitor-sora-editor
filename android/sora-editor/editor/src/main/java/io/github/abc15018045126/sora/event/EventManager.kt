package io.github.abc15018045126.sora.event

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.LinkedList
import java.util.Vector
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


class EventManager @JvmOverloads constructor(private val parent: EventManager? = null) {

    private val receiversMap: MutableMap<Class<*>, Receivers<*>> = HashMap()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val children: MutableList<EventManager> = Vector()
    private val caches: Array<Array<EventReceiver<*>>?> = arrayOfNulls(5)

    var isEnabled: Boolean = true

        set(value) {
            if (parent == null && !value) {
                throw IllegalStateException("The event manager is set to be root, and can not be disabled")
            }
            field = value
        }

    private var detached: Boolean = false

    init {
        parent?.children?.add(this)
    }


    val rootManager: EventManager
        get() {
            checkDetached()
            return parent?.rootManager ?: this
        }


    fun <T : Event> dispatchEventFromRoot(event: T): Int {
        return rootManager.dispatchEvent(event)
    }


    fun detach() {
        if (parent == null) {
            throw IllegalStateException("root manager can not be detached")
        }
        checkDetached()
        detached = true
        parent.children.remove(this)
    }

    private fun checkDetached() {
        if (detached) {
            throw IllegalStateException("already detached")
        }
    }


    @Suppress("UNCHECKED_CAST")
    internal fun <T : Event> getReceivers(type: Class<T>): Receivers<T> {
        lock.readLock().lock()
        var result: Receivers<T>?
        try {
            result = receiversMap[type] as Receivers<T>?
        } finally {
            lock.readLock().unlock()
        }
        if (result == null) {
            lock.writeLock().lock()
            try {
                result = receiversMap[type] as Receivers<T>?
                if (result == null) {
                    result = Receivers()
                    receiversMap[type] = result
                }
            } finally {
                lock.writeLock().unlock()
            }
        }
        return result!!
    }


    fun <T : Event> subscribe(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> {
        return subscribeEvent(eventType, receiver)
    }


    fun <T : Event> subscribeAlways(
        eventType: Class<T>,
        receiver: NoUnsubscribeReceiver<T>
    ): SubscriptionReceipt<T> {
        return subscribeEvent(eventType) { event, _ -> receiver.onEvent(event) }
    }


    fun <T : Event> subscribeEvent(
        eventType: Class<T>,
        receiver: EventReceiver<T>
    ): SubscriptionReceipt<T> {
        val receivers = getReceivers(eventType)
        receivers.lock.writeLock().lock()
        try {
            val list = receivers.receivers
            if (!list.contains(receiver)) {
                list.add(receiver)
            }
        } finally {
            receivers.lock.writeLock().unlock()
        }
        return SubscriptionReceipt(this, eventType, receiver)
    }


    @Suppress("UNCHECKED_CAST")
    fun <T : Event> dispatchEvent(event: T): Int {
        if (!isEnabled) {
            return event.interceptTargets
        }

        val receivers = getReceivers(event.javaClass as Class<T>)
        receivers.lock.readLock().lock()
        var receiverArr: Array<EventReceiver<T>>
        var count: Int
        try {
            count = receivers.receivers.size
            receiverArr = obtainBuffer(count)
            (receivers.receivers as ArrayList<EventReceiver<T>>).toArray(receiverArr)
        } finally {
            receivers.lock.readLock().unlock()
        }
        var unsubscribedReceivers: MutableList<EventReceiver<T>>? = null
        try {
            val unsubscribe = Unsubscribe()
            var i = 0
            while (i < count && (event.interceptTargets and InterceptTarget.TARGET_RECEIVERS) == 0) {
                val receiver = receiverArr[i]
                receiver.onReceive(event, unsubscribe)
                if (unsubscribe.isUnsubscribed) {
                    if (unsubscribedReceivers == null) {
                        unsubscribedReceivers = LinkedList()
                    }
                    unsubscribedReceivers.add(receiver)
                }
                unsubscribe.reset()
                i++
            }
        } finally {
            if (unsubscribedReceivers != null) {
                receivers.lock.writeLock().lock()
                try {
                    receivers.receivers.removeAll(unsubscribedReceivers!!)
                } finally {
                    receivers.lock.writeLock().unlock()
                }
            }
            recycleBuffer(receiverArr)
        }
        var j = 0
        while (j < children.size && (event.interceptTargets and InterceptTarget.TARGET_RECEIVERS) == 0) {
            var sub: EventManager? = null
            try {
                sub = children[j]
            } catch (e: IndexOutOfBoundsException) {

            }
            sub?.dispatchEvent(event)
            j++
        }
        return event.interceptTargets
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V : Event> obtainBuffer(size: Int): Array<EventReceiver<V>> {
        var res: Array<EventReceiver<V>>? = null
        synchronized(this) {
            for (i in caches.indices) {
                if (caches[i] != null && caches[i]!!.size >= size) {
                    res = caches[i] as Array<EventReceiver<V>>?
                    caches[i] = null
                    break
                }
            }
        }
        if (res == null) {
            res = java.lang.reflect.Array.newInstance(EventReceiver::class.java, size) as Array<EventReceiver<V>>
        }
        return res!!
    }

    @Synchronized
    private fun recycleBuffer(array: Array<out EventReceiver<*>>?) {
        if (array == null) {
            return
        }
        for (i in caches.indices) {
            if (caches[i] == null) {
                Arrays.fill(array, null)
                caches[i] = array as Array<EventReceiver<*>>
                break
            }
        }
    }


    internal class Receivers<T : Event> {
        val lock: ReadWriteLock = ReentrantReadWriteLock()
        val receivers: MutableList<EventReceiver<T>> = ArrayList()
    }

    fun interface NoUnsubscribeReceiver<T : Event> {
        fun onEvent(event: T)
    }
}
