package io.github.abc15018045126.sora.annotations

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.CLASS
)
annotation class UnsupportedUserUsage
