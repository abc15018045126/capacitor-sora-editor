package io.github.abc15018045126.sora.event

import java.lang.ref.WeakReference


class SubscriptionReceipt<R : Event> internal constructor(
    private val manager: EventManager,
    private val clazz: Class<R>,
    receiver: EventReceiver<R>
) {
    private val receiver: WeakReference<EventReceiver<R>> = WeakReference(receiver)


    fun unsubscribe() {
        val receivers = manager.getReceivers(clazz)
        receivers.lock.writeLock().lock()
        try {
            val target = receiver.get()
            if (target != null) {
                receivers.receivers.remove(target)
            }
        } finally {
            receivers.lock.writeLock().unlock()
        }
    }
}
