

package io.github.abc15018045126.sora.event

fun ResultedEvent<Boolean>.getResultBoolean(): Boolean = if (isResultSet) {
    result!!
} else {
    false
}

inline fun <reified T : Event> EventManager.subscribeEvent(receiver: EventReceiver<T>) =
    subscribeEvent(T::class.java, receiver)

inline fun <reified T : Event> EventManager.subscribeAlways(receiver: EventManager.NoUnsubscribeReceiver<T>) =
    subscribeAlways(T::class.java, receiver)
