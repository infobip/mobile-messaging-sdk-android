/*
 * NotificationActionTapReceiver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.platform.AndroidInteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.RemoteInput;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;


public class NotificationActionTapReceiver extends BroadcastReceiver {

    private InteractiveBroadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;
    private MobileInteractive mobileInteractive;
    private ActivityStarterWrapper activityStarterWrapper;

    public NotificationActionTapReceiver() {
    }

    @VisibleForTesting
    public NotificationActionTapReceiver(InteractiveBroadcaster broadcaster, MobileMessagingCore mobileMessagingCore, MobileInteractive mobileInteractive, ActivityStarterWrapper activityStarterWrapper) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.mobileInteractive = mobileInteractive;
        this.activityStarterWrapper = activityStarterWrapper;
    }

    @Override
    public void onReceive(Context context, Intent receivedIntent) {
        Bundle actionBundle = receivedIntent.getBundleExtra(EXTRA_TAPPED_ACTION);
        Bundle categoryBundle = receivedIntent.getBundleExtra(EXTRA_TAPPED_CATEGORY);
        int notificationId = receivedIntent.getIntExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, -1);
        Bundle messageBundle = receivedIntent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);

        cancelNotification(context, notificationId);

        Message message = Message.createFrom(messageBundle);
        if (message == null) {
            MobileMessagingLogger.w("Received no message in NotificationActionTapReceiver");
            return;
        }

        NotificationAction notificationAction = NotificationAction.createFrom(actionBundle);
        if (notificationAction == null) {
            MobileMessagingLogger.w("Received no action in NotificationActionTapReceiver");
            return;
        }

        NotificationCategory notificationCategory = NotificationCategory.createFrom(categoryBundle);
        if (notificationCategory == null) {
            MobileMessagingLogger.w("Received no notification category in NotificationActionTapReceiver");
            return;
        }

        String inputText = getInputTextFromIntent(receivedIntent, notificationAction);
        if (inputText != null) {
            notificationAction.setInputText(inputText);
        }

        Intent callbackIntent = broadcaster(context).notificationActionTapped(message, notificationCategory, notificationAction);
        mobileInteractive(context).triggerSdkActionsFor(notificationAction, message);

        if (notificationAction.bringsAppToForeground()) {
            activityStarterWrapper(context).startCallbackActivity(callbackIntent);
        }
    }

    private String getInputTextFromIntent(Intent intent, NotificationAction notificationAction) {
        if (notificationAction == null || !notificationAction.hasInput()) {
            return null;
        }

        Bundle input = RemoteInput.getResultsFromIntent(intent);
        if (input == null) {
            return null;
        }

        CharSequence sequence = input.getCharSequence(notificationAction.getId());
        return sequence != null ? sequence.toString() : "";
    }

    private void cancelNotification(Context context, int notificationId) {
        if (notificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(notificationId);
            }
        }
    }

    private InteractiveBroadcaster broadcaster(Context context) {
        if (broadcaster == null) {
            broadcaster = new AndroidInteractiveBroadcaster(context);
        }
        return broadcaster;
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }

    private MobileInteractive mobileInteractive(Context context) {
        if (mobileInteractive == null) {
            mobileInteractive = MobileInteractiveImpl.getInstance(context);
        }
        return mobileInteractive;
    }

    private ActivityStarterWrapper activityStarterWrapper(Context context) {
        if (activityStarterWrapper == null) {
            activityStarterWrapper = new ActivityStarterWrapper(context, mobileMessagingCore(context));
        }
        return activityStarterWrapper;
    }
}
