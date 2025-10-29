/*
 * BackendCommunicationException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiIOException;

/**
 * @author sslavin
 * @since 25/07/2017.
 */

public class BackendCommunicationException extends BackendBaseException {
    public BackendCommunicationException(String message, ApiIOException cause) {
        super(message, cause);
    }
}
