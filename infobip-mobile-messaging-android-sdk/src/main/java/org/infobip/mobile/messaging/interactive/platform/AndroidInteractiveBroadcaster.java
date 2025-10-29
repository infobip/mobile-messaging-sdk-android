/*
 * AndroidInteractiveBroadcaster.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.platform;

import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.interactive.InteractiveEvent;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public class AndroidInteractiveBroadcaster implements InteractiveBroadcaster {

    private final Context context;

    public AndroidInteractiveBroadcaster(Context context) {
        this.context = context;
    }

    @Override
    public Intent notificationActionTapped(Message message, NotificationCategory category, NotificationAction action) {
        Intent actionTapped = prepare(InteractiveEvent.NOTIFICATION_ACTION_TAPPED);
        actionTapped.putExtras(MessageBundleMapper.messageToBundle(message));
        actionTapped.putExtras(NotificationActionBundleMapper.notificationActionToBundle(action));
        actionTapped.putExtras(NotificationCategoryBundleMapper.notificationCategoryToBundle(category));

        send(actionTapped);

        return actionTapped;
    }

    @Override
    public void inAppNotificationIsReadyToDisplay(Message message) {
        Intent intent = prepare(InteractiveEvent.MODAL_IN_APP_NOTIFICATION_IS_READY_TO_DISPLAY).putExtras(MessageBundleMapper.messageToBundle(message));
        send(intent);
    }

    private void send(Intent intent) {
        try {
            context.sendBroadcast(intent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception ex) {
            MobileMessagingLogger.e("Failed to send broadcast for action " + intent.getAction() + " due to exception " + ex.getMessage(), ex);
        }
    }

    private Intent prepare(InteractiveEvent event) {
        return prepare(event.getKey());
    }

    private Intent prepare(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
