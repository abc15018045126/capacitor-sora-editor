
package io.github.abc15018045126.sora.event;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

/**
 * Receipt of {@link EventManager#subscribeEvent(Class, EventReceiver)}. You can unsubscribe the event outside
 * the dispatch process from any thread by calling {@link SubscriptionReceipt#unsubscribe()}
 *
 * @author abc15018045126
 */
public class SubscriptionReceipt<R extends Event> {

    private final Class<R> clazz;
    private final WeakReference<EventReceiver<R>> receiver;
    private final EventManager manager;

    SubscriptionReceipt(@NonNull EventManager mgr, @NonNull Class<R> clazz, @NonNull EventReceiver<R> receiver) {
        this.clazz = clazz;
        this.receiver = new WeakReference<>(receiver);
        this.manager = mgr;
    }

    /**
     * Unsubscribe the event receiver.
     * <p>
     * Does nothing if the listener is already recycled or unsubscribed.
     */
    public void unsubscribe() {
        var receivers = manager.getReceivers(clazz);
        receivers.lock.writeLock().lock();
        try {
            var target = receiver.get();
            if (target != null) {
                receivers.receivers.remove(target);
            }
        } finally {
            receivers.lock.writeLock().unlock();
        }
    }

}

