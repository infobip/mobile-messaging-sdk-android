package org.infobip.mobile.messaging.app;


import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class CallbackActivityStarterWrapper {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;

    public CallbackActivityStarterWrapper(Context context, MobileMessagingCore mobileMessagingCore) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    /**
     * Starts activity
     *
     * @param callbackIntent       Intent with extras/actions etc. to forward to the callback activity
     */
    public void startActivity(Intent callbackIntent) {
        NotificationSettings notificationSettings = mobileMessagingCore.getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        Class callbackActivity = notificationSettings.getCallbackActivity();
        if (callbackActivity == null) {
            MobileMessagingLogger.e("Callback activity is not set, cannot proceed");
            return;
        }

        int intentFlags = notificationSettings.getIntentFlags();
        // FLAG_ACTIVITY_NEW_TASK has to be here because we're starting activity outside of activity context
        intentFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;

        callbackIntent.addFlags(intentFlags);
        callbackIntent.setClass(context, callbackActivity);
        context.startActivity(callbackIntent);
    }
}
