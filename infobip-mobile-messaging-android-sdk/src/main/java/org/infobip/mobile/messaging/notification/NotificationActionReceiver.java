package org.infobip.mobile.messaging.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.InteractiveCategoryBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_ACTION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TRIGGERED_CATEGORY;


public class NotificationActionReceiver extends BroadcastReceiver {

    private Broadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;

    public NotificationActionReceiver() {
    }

    @VisibleForTesting
    public NotificationActionReceiver(Broadcaster broadcaster, MobileMessagingCore mobileMessagingCore) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionId = intent.getStringExtra(EXTRA_TRIGGERED_ACTION_ID);
        Bundle categoryBundle = intent.getBundleExtra(EXTRA_TRIGGERED_CATEGORY);
        int notificationId = intent.getIntExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, -1);
        InteractiveCategory interactiveCategory = InteractiveCategory.createFrom(categoryBundle);

        if (notificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }

        broadcaster(context).notificationActionTriggered(interactiveCategory, actionId);

        startCallbackActivity(context, intent, actionId, interactiveCategory);
    }

    private void startCallbackActivity(Context context, Intent intent, String actionId, InteractiveCategory interactiveCategory) {
        boolean bringAppToForeground = false;
        for (NotificationAction notificationAction : interactiveCategory.getNotificationActions()) {
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
        callbackIntent.putExtra(EXTRA_TRIGGERED_ACTION_ID, actionId);
        callbackIntent.putExtra(EXTRA_TRIGGERED_CATEGORY, InteractiveCategoryBundleMapper.interactiveCategoryToBundle(interactiveCategory));

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
