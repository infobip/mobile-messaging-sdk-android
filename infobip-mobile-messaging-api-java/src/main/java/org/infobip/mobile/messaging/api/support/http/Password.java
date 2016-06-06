package org.infobip.mobile.messaging.api.support.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Sets password.
 */
@Documented
@Target({METHOD, TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface Password {
    String value() default "";
}
