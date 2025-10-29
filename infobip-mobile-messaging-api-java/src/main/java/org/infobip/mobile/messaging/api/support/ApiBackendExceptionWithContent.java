/*
 * ApiBackendExceptionWithContent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

/**
 * @author sslavin
 * @since 17/10/2017.
 */

public class ApiBackendExceptionWithContent extends ApiIOException {

    private final Object content;

    public ApiBackendExceptionWithContent(String code, String message, Object content) {
        super(code, message);
        this.content = content;
    }

    public Object getContent() {
        return content;
    }
}
