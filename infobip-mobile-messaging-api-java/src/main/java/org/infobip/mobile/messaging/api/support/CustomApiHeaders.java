/*
 * CustomApiHeaders.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

/**
 * @author pandric on 10/04/2017.
 */

public enum CustomApiHeaders {
    FOREGROUND("foreground"),
    SESSION_ID("sessionId"),
    PUSH_REGISTRATION_ID("pushregistrationid"),
    INSTALLATION_ID("installationid"),
    NEW_BASE_URL("New-Base-URL"),
    APPLICATION_CODE("applicationcode");

    private final String value;

    CustomApiHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
