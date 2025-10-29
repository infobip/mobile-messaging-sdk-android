/*
 * SystemTimeProvider.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.platform;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class SystemTimeProvider implements TimeProvider {
    @Override
    public long now() {
        return System.currentTimeMillis();
    }
}
