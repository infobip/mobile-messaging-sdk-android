/*
 * BackendBaseException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;

/**
 * @author sslavin
 * @since 26/07/2017.
 */

public abstract class BackendBaseException extends RuntimeException {

    public BackendBaseException(String message, ApiIOException cause) {
        super(message, cause);
    }

    /**
     * Creates error based on exception contents.
     * @return mobile messaging error.
     */
    public MobileMessagingError getError() {
        return MobileMessagingError.createFrom(getCause());
    }
}
