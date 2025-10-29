/*
 * ConfigurationException.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

/**
 * Thrown when MobileMessaging SDK configuration is wrong and action described
 * in error message needs to be taken to fix this.
 *
 * @see java.lang.RuntimeException
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String detailMessage) {
        super(detailMessage);
    }

    public ConfigurationException(Reason reason) {
        super(reason.message());
    }

    public ConfigurationException(Reason reason, String component) {
        super(String.format(reason.message(), component));
    }

    public ConfigurationException(Throwable throwable) {
        super(throwable);
    }

    public ConfigurationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public enum Reason {
        MISSING_REQUIRED_COMPONENT("Missing required component in AndroidManifest.xml, add: %s, check all needed components in https://github.com/infobip/mobile-messaging-sdk-android/wiki/Android-Manifest-components#push-notifications"),
        MISSING_REQUIRED_PERMISSION("Missing required permission in AndroidManifest.xml, add: %s"),
        CHECK_LOCATION_SETTINGS("Check your location settings.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        public String message() {
            return this.message;
        }

    }
}
