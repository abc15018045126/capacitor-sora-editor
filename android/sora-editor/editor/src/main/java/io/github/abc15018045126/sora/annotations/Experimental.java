
package io.github.abc15018045126.sora.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks those fields, methods and constructors experimentally created.
 * <p>
 * Methods, fields and constructors with this annotation is very subject to keep or delete.
 * For that reason, they are not stable for production use.
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Experimental {
}

