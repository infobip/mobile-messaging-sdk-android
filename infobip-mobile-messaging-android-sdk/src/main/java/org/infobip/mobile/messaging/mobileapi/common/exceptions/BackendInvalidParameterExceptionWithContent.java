/*
 * BackendInvalidParameterExceptionWithContent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;

/**
 * @author sslavin
 * @since 17/10/2017.
 */

public class BackendInvalidParameterExceptionWithContent extends BackendBaseExceptionWithContent {
    public BackendInvalidParameterExceptionWithContent(String message, ApiBackendExceptionWithContent cause) {
        super(message, cause);
    }
}
