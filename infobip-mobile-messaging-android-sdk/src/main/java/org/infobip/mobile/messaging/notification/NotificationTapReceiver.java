package org.infobip.mobile.messaging.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;

/**
 * @author sslavin
 * @since 22/02/2017.
 */

public class NotificationTapReceiver extends BroadcastReceiver {

    private Broadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;

    public NotificationTapReceiver() {}

    public NotificationTapReceiver(Broadcaster broadcaster, MobileMessagingCore mobileMessagingCore) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle messageBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);
        Message message = Message.createFrom(messageBundle);
        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationTapReceiver");
            return;
        }

        broadcaster(context).notificationTapped(message);

        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }

        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(message.getMessageId());
        }

        Class callbackActivity = notificationSettings.getCallbackActivity();
        if (callbackActivity == null) {
            MobileMessagingLogger.e("Callback activity is not set, cannot proceed");
            return;
        }

        int intentFlags = intent.getIntExtra(MobileMessagingProperty.EXTRA_INTENT_FLAGS.getKey(),
                (Integer) MobileMessagingProperty.INTENT_FLAGS.getDefaultValue());

        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.putExtra(BroadcastParameter.EXTRA_MESSAGE, messageBundle);
        // FLAG_ACTIVITY_NEW_TASK has to be here
        // because we're starting activity outside of activity context
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
