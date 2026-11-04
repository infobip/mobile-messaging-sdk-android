/*
 * Path.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Substitutes a named placeholder in the request path.
 */
@Documented
@Target({PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface Path {
    String name() default "";
    String value() default "";

    /**
     * Percent-encodes the raw value as one UTF-8 path segment before substitution.
     * Slashes, semicolons (matrix-parameter delimiters), query/fragment delimiters
     * and literal percent signs are encoded;
     * callers must not pre-encode values when this option is enabled.
     * Defaults to false to preserve existing direct-substitution behavior.
     */
    boolean encode() default false;
}