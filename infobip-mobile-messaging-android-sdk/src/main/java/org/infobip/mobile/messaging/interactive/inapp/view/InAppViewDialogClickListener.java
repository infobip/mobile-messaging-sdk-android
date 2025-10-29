/*
 * InAppViewDialogClickListener.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author sslavin
 * @since 12/04/2018.
 */
public class InAppViewDialogClickListener implements DialogInterface.OnClickListener {
    private final InAppNativeView inAppView;
    private final InAppView.Callback callback;
    private final Message message;
    private final NotificationCategory category;
    private final NotificationAction action;

    InAppViewDialogClickListener(InAppNativeView inAppView, InAppView.Callback callback, Message message, NotificationCategory category, NotificationAction action) {
        this.inAppView = inAppView;
        this.callback = callback;
        this.message = message;
        this.category = category;
        this.action = action;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        callback.buttonPressedFor(inAppView, message, category, action);
    }
}
