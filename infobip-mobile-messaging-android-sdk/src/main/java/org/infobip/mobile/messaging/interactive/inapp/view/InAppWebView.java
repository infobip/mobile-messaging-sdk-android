/*
 * InAppWebView.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;

import androidx.annotation.NonNull;

public interface InAppWebView extends InAppView {
    void show(@NonNull InAppWebViewMessage message, @NonNull NotificationAction... actions);
}
