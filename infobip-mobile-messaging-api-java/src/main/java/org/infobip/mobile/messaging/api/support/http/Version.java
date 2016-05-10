package org.infobip.mobile.messaging.api.support.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Make a HTTP request.
 */
@Documented
@Target({PACKAGE, TYPE, METHOD})
@Retention(RUNTIME)
public @interface Version {
    String value() default "";
}