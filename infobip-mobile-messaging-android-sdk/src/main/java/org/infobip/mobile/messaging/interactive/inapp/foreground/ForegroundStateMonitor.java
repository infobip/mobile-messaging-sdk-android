/*
 * ForegroundStateMonitor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.foreground;

import androidx.annotation.NonNull;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public interface ForegroundStateMonitor {
    @NonNull ForegroundState isInForeground();
}
