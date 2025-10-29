/*
 * InAppNativeView.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import android.graphics.Bitmap;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

import androidx.annotation.NonNull;

public interface InAppNativeView extends InAppView {
    void show(@NonNull Message message, NotificationCategory category, @NonNull NotificationAction... actions);
    void showWithImage(@NonNull Bitmap bitmap, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction... actions);
}
