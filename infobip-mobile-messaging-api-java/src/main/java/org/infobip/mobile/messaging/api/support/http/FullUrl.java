package org.infobip.mobile.messaging.api.support.http;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Make a HTTP request.
 */
@Documented
@Target({PARAMETER})
@Retention(RUNTIME)
public @interface FullUrl {

    String value() default "";

}
