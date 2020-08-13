package org.infobip.mobile.messaging.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 22/02/2017.
 */

public class NotificationTapReceiver extends BroadcastReceiver {

    private Broadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;
    private ActivityStarterWrapper activityStarterWrapper;

    public NotificationTapReceiver() {
    }

    @VisibleForTesting
    public NotificationTapReceiver(Broadcaster broadcaster, MobileMessagingCore mobileMessagingCore, ActivityStarterWrapper activityStarterWrapper) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.activityStarterWrapper = activityStarterWrapper;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle messageBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);
        Message message = Message.createFrom(messageBundle);
        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationTapReceiver");
            return;
        }

        for (MessageHandlerModule module : mobileMessagingCore(context).getMessageHandlerModules()) {
            if (module.messageTapped(message)) {
                return;
            }
        }

        broadcaster(context).notificationTapped(message);

        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }

        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(message.getMessageId());
        }

        Intent callbackIntent = new Intent(intent);
        callbackIntent.setAction(Event.NOTIFICATION_TAPPED.getKey());

        if (StringUtils.isNotBlank(message.getWebViewUrl())) {
            activityStarterWrapper(context).startWebViewActivity(callbackIntent, message.getWebViewUrl());
        } else if (StringUtils.isNotBlank(message.getBrowserUrl())) {
            activityStarterWrapper(context).startBrowser(message.getBrowserUrl());
        } else {
            activityStarterWrapper(context).startCallbackActivity(callbackIntent);
        }
    }

    private Broadcaster broadcaster(Context context) {
        if (broadcaster == null) {
            broadcaster = new AndroidBroadcaster(context);
        }
        return broadcaster;
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }

    private ActivityStarterWrapper activityStarterWrapper(Context context) {
        if (activityStarterWrapper == null) {
            activityStarterWrapper = new ActivityStarterWrapper(context, mobileMessagingCore(context));
        }
        return activityStarterWrapper;
    }
}
