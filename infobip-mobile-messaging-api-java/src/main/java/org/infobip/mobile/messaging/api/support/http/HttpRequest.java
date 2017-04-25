package org.infobip.mobile.messaging.api.support.http;

import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Make a HTTP request.
 */
@Documented
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface HttpRequest {

    HttpMethod method() default HttpMethod.GET;

    /**
     * A relative or absolute path, or full URL of the endpoint.
     */
    String value() default "";
}