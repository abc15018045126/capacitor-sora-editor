package io.github.abc15018045126.sora.event


class Unsubscribe {
    var isUnsubscribed: Boolean = false
        private set


    fun unsubscribe() {
        isUnsubscribed = true
    }


    fun reset() {
        isUnsubscribed = false
    }
}
