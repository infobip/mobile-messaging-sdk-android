/*
 * JwtExpirationException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

public class JwtExpirationException extends RuntimeException {
    private static final String message = "The provided JWT is expired.";

    public JwtExpirationException() {
        super(message);
    }
}
