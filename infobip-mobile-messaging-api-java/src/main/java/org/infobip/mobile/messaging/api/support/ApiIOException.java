/*
 * ApiIOException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

import org.infobip.mobile.messaging.api.support.util.StringUtils;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class ApiIOException extends RuntimeException {
    private final String code;

    public ApiIOException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ApiIOException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String toString() {
        String s = getClass().getName();
        String message = getLocalizedMessage();
        String code = getCode();
        if (StringUtils.isNotBlank(code)) {
            if (StringUtils.isNotBlank(message)) {
                message = code + ", "+ message;
            } else {
                message = code;
            }
        }
        return (message != null) ? (s + ": " + message) : s;
    }
}
