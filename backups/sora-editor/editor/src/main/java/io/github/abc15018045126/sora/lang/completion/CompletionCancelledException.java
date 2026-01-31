
package io.github.abc15018045126.sora.lang.completion;

/**
 * Thrown when the thread is abandoned by the editor framework because the editor do not need its
 * new items anymore.
 * <p>
 * This can be thrown by {@link io.github.abc15018045126.sora.text.ContentReference} and
 * {@link CompletionPublisher}.
 *
 * @author abc15018045126
 */
public class CompletionCancelledException extends RuntimeException {

    public CompletionCancelledException() {
    }

    public CompletionCancelledException(String message) {
        super(message);
    }
}

