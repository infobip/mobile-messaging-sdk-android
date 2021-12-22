package org.infobip.mobile.messaging.app;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.view.WebViewActivity;

public class ActivityStarterWrapper {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;

    public ActivityStarterWrapper(Context context, MobileMessagingCore mobileMessagingCore) {
        this.context = context;
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

    /**
     * Starts web view activity
     *
     * @param webViewIntent Intent with extras/actions etc. to forward to the web view activity
     */
    public void startWebViewActivity(@NonNull Intent webViewIntent, @NonNull String url) {
        if (WebViewActivity.canOpenURLWithOtherApp(url, context)) return;
        webViewIntent.putExtra(WebViewActivity.EXTRA_URL, url);
        webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        webViewIntent.setClass(context, WebViewActivity.class);
        context.startActivity(webViewIntent);
    }

    /**
     * Starts available browser
     */
    public void startBrowser(@NonNull String browserUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }
}
