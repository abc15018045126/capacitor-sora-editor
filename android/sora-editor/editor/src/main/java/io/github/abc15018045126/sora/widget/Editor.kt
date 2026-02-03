

package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.event.Event
import io.github.abc15018045126.sora.event.EventManager.NoUnsubscribeReceiver
import io.github.abc15018045126.sora.event.EventReceiver
import io.github.abc15018045126.sora.event.SubscriptionReceipt
import io.github.abc15018045126.sora.widget.component.EditorBuiltinComponent


inline fun <reified T : Event> CodeEditor.subscribeAlways(receiver: NoUnsubscribeReceiver<T>): SubscriptionReceipt<T> {
    return subscribeAlways(T::class.java, receiver)
}


inline fun <reified T : Event> CodeEditor.subscribeEvent(receiver: EventReceiver<T>): SubscriptionReceipt<T> {
    return subscribeEvent(T::class.java, receiver)
}


inline fun <reified T : EditorBuiltinComponent> CodeEditor.getComponent(): T {
    return getComponent(T::class.java)
}


inline fun <reified T : EditorBuiltinComponent> CodeEditor.replaceComponent(component: T) {
    replaceComponent(T::class.java, component)
}
