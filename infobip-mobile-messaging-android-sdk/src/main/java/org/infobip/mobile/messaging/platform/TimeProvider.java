/*
 * TimeProvider.java
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

public interface TimeProvider {
    /**
     * Returns the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC (equivalent to Time.now())
     * @return current time in milliseconds
     */
    long now();
}
