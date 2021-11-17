package org.infobip.mobile.messaging.app;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;

public class ActivityStarterWrapper extends ContentIntentWrapper {

    private final MobileMessagingCore mobileMessagingCore;

    public ActivityStarterWrapper(Context context, MobileMessagingCore mobileMessagingCore) {
        super(context);
        this.mobileMessagingCore = mobileMessagingCore;
    }

    /**
     * Starts activity
     *
     * @param callbackIntent Intent with extras/actions etc. to forward to the callback activity
     */
    public void startCallbackActivity(@NonNull Intent callbackIntent) {
        NotificationSettings notificationSettings = mobileMessagingCore.getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }

        Intent intent = createContentIntent(callbackIntent, notificationSettings);
        if (intent == null) {
            return;
        }

        context.startActivity(callbackIntent);
    }

    /**
     * Starts web view activity
     *
     * @param webViewIntent Intent with extras/actions etc. to forward to the web view activity
     */
    public void startWebViewActivity(@NonNull Intent webViewIntent, @NonNull String url) {
        Intent intent = createWebViewContentIntent(webViewIntent, url);
        context.startActivity(intent);
    }

    /**
     * Starts available browser
     */
    public void startBrowser(@NonNull String browserUrl) {
        Intent intent = createBrowserIntent(browserUrl);
        if (intent != null) {
            context.startActivity(intent);
        }
    }
}
