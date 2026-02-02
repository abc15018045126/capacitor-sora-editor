package io.github.abc15018045126.sora.annotations

/**
 * Marks you must call {@link View#invalidate()} after changing this property
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FIELD)
annotation class InvalidateRequired
