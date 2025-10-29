/*
 * NoHttpRequestAnnotation.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class NoHttpRequestAnnotation extends RuntimeException {
    public NoHttpRequestAnnotation(String message) {
        super(message);
    }

    public NoHttpRequestAnnotation(String message, Throwable cause) {
        super(message, cause);
    }
}
