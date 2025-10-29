/*
 * LocalEvent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

public enum LocalEvent {

    /**
     * It is triggered when Application goes in foreground.
     */
    APPLICATION_FOREGROUND("org.infobip.mobile.messaging.APPLICATION_FOREGROUND");

    private final String key;

    LocalEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
