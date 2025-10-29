/*
 * ApiServiceException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
@SuppressWarnings("DefaultAnnotationParam")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ApiServiceException extends RuntimeException {
    private String messageId;
    private String text;

    @Override
    public String getMessage() {
        return getText();
    }

    @Override
    public String getLocalizedMessage() {
        return getText();
    }
}
