/*
 * UserDataValidationException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.user;

/**
 * Exception thrown when user data validation fails against People API field limits.
 *
 * @see UserDataValidator
 */
public class UserDataValidationException extends RuntimeException {

    public UserDataValidationException(String message) {
        super(message);
    }

    public UserDataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
