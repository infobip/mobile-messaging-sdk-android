/*
 * Writer.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.logging;

import androidx.annotation.Nullable;

/**
 * @author sslavin
 * @since 11/07/2017.
 */

public interface Writer {
    void write(Level level, String tag, String message, @Nullable Throwable throwable);
}
