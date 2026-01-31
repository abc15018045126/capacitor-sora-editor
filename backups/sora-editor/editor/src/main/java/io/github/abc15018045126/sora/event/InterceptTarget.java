
package io.github.abc15018045126.sora.event;

/**
 * Define available intercept targets. You may intercept one or more targets of the given event.
 *
 * @author abc15018045126
 */
public interface InterceptTarget {

    /**
     * Registered receivers in the event dispatching graph
     */
    int TARGET_RECEIVERS = 1;

    /**
     * Editor built-in behavior
     */
    int TARGET_EDITOR = 1 << 1;

}

