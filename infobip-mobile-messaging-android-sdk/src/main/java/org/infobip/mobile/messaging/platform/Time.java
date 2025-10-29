/*
 * Time.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.platform;

import java.util.Date;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class Time {

    private static TimeProvider timeProvider = new SystemTimeProvider();

    public static void reset(TimeProvider timeProvider) {
        Time.timeProvider = timeProvider;
    }

    public static long now() {
        return timeProvider.now();
    }

    public static Date date() {
        return new Date(timeProvider.now());
    }
}
