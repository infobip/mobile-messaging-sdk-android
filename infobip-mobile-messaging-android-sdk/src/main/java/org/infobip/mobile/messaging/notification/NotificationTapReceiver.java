package org.infobip.mobile.messaging.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;

/**
 * @author sslavin
 * @since 22/02/2017.
 */

public class NotificationTapReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle messageBundle = intent.getBundleExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey());
        Message message = Message.createFrom(messageBundle);
        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationTapReceiver");
            return;
        }

        MobileMessaging.getInstance(context).setMessagesSeen(message.getMessageId());

        Class callbackActivity = (Class) intent.getSerializableExtra(MobileMessagingProperty.EXTRA_CALLBACK_ACTIVITY.getKey());
        if (callbackActivity == null) {
            MobileMessagingLogger.e("Callback activity is not set, cannot proceed");
            return;
        }

        int intentFlags = intent.getIntExtra(MobileMessagingProperty.EXTRA_INTENT_FLAGS.getKey(),
                (Integer) MobileMessagingProperty.INTENT_FLAGS.getDefaultValue());

        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), messageBundle);
        // FLAG_ACTIVITY_NEW_TASK has to be here
        // because we're starting activity outside of activity context
        callbackIntent.addFlags(intentFlags | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callbackIntent);
    }
}
