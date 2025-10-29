/*
 * MessageStoreInstantiationException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

/**
 * @author mstipanov
 * @since 30.03.2016.
 */
public class MessageStoreInstantiationException extends RuntimeException {
    public MessageStoreInstantiationException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
