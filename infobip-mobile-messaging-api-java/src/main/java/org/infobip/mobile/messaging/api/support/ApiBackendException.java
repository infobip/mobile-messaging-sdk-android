/*
 * ApiBackendException.java
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
public class ApiBackendException extends ApiIOException {
    public ApiBackendException(String code, String message) {
        super(code, message);
    }

    public ApiBackendException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
