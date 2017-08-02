package org.infobip.mobile.messaging.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationAction;
import org.infobip.mobile.messaging.NotificationCategory;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;


public class NotificationActionTapReceiver extends BroadcastReceiver {

    private Broadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;

    public NotificationActionTapReceiver() {
    }

    @VisibleForTesting
    public NotificationActionTapReceiver(Broadcaster broadcaster, MobileMessagingCore mobileMessagingCore) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionId = intent.getStringExtra(EXTRA_TAPPED_ACTION_ID);
        Bundle categoryBundle = intent.getBundleExtra(EXTRA_TAPPED_CATEGORY);
        int notificationId = intent.getIntExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, -1);
        Bundle messageBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);

        Message message = Message.createFrom(messageBundle);
        NotificationCategory notificationCategory = NotificationCategory.createFrom(categoryBundle);
        cancelNotification(context, notificationId);

        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationActionTapReceiver");
            return;
        }
        if (actionId == null) {
            MobileMessagingLogger.e("Received no action ID in NotificationActionTapReceiver");
            return;
        }
        if (notificationCategory == null) {
            MobileMessagingLogger.e("Received no notification category in NotificationActionTapReceiver");
            return;
        }

        broadcaster(context).notificationActionTapped(message, notificationCategory, actionId);

        markAsSeen(context, message);
        startCallbackActivity(context, intent, messageBundle, actionId, notificationCategory);
    }

    private void markAsSeen(Context context, Message message) {
        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(message.getMessageId());
        }
    }

    private void cancelNotification(Context context, int notificationId) {
        if (notificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    private void startCallbackActivity(Context context, Intent intent, Bundle messageBundle, String actionId, NotificationCategory notificationCategory) {
        boolean bringAppToForeground = false;
        for (NotificationAction notificationAction : notificationCategory.getNotificationActions()) {
            if (actionId.equals(notificationAction.getId())) {
                bringAppToForeground = notificationAction.bringsAppToForeground();
                break;
            }
        }

        if (!bringAppToForeground) {
            return;
        }

        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        Class callbackActivity = notificationSettings.getCallbackActivity();
        if (callbackActivity == null) {
            MobileMessagingLogger.e("Callback activity is not set, cannot proceed");
            return;
        }

        int intentFlags = intent.getIntExtra(MobileMessagingProperty.EXTRA_INTENT_FLAGS.getKey(),
                (Integer) MobileMessagingProperty.INTENT_FLAGS.getDefaultValue());

        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.putExtra(EXTRA_MESSAGE, messageBundle);
        callbackIntent.putExtra(EXTRA_TAPPED_ACTION_ID, actionId);
        callbackIntent.putExtra(EXTRA_TAPPED_CATEGORY, NotificationCategoryBundleMapper.notificationCategoryToBundle(notificationCategory));

        // FLAG_ACTIVITY_NEW_TASK has to be here because we're starting activity outside of activity context
        callbackIntent.addFlags(intentFlags | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callbackIntent);
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
}
