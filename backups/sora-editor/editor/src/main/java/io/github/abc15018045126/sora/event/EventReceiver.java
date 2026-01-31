
package io.github.abc15018045126.sora.event;

import androidx.annotation.NonNull;

public interface EventReceiver<T extends Event> {

    void onReceive(@NonNull T event, @NonNull Unsubscribe unsubscribe);

}

