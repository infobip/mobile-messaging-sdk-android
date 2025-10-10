package org.infobip.mobile.messaging.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.view.WebViewActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;

public class ContentIntentWrapper {
    protected final Context context;

    public ContentIntentWrapper(Context context) {
        this.context = context;
    }

    /**
     * Creates content intent for PendingIntent
     */
    @Nullable
    public Intent createContentIntent(@NonNull Message message, @NonNull NotificationSettings notificationSettings) {
        Intent intent = createContentIntent(new Intent(), notificationSettings);
        if (intent == null) return null;

        intent.setAction(Event.NOTIFICATION_TAPPED.getKey());
        intent.putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message));
        return intent;
    }

    @Nullable
    public Intent createContentIntent(@NonNull Intent intent, @NonNull NotificationSettings notificationSettings) {
        Class callbackActivity = notificationSettings.getCallbackActivity();
        if (callbackActivity == null) {
            MobileMessagingLogger.w("Callback activity is not set, cannot proceed");
            return null;
        }

        int intentFlags = notificationSettings.getIntentFlags();
        // FLAG_ACTIVITY_NEW_TASK has to be here because we're starting activity outside of activity context
        intentFlags |= Intent.FLAG_ACTIVITY_NEW_TASK;

        intent.addFlags(intentFlags);
        intent.setClass(context, callbackActivity);

        return intent;
    }

    /**
     * Creates content intent with webView for PendingIntent
     */
    public Intent createWebViewContentIntent(@NonNull Message message) {
        Intent webViewIntent = createWebViewContentIntent(new Intent(), message.getWebViewUrl());
        webViewIntent.setAction(Event.NOTIFICATION_TAPPED.getKey());
        webViewIntent.putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message));
        return webViewIntent;
    }

    public Intent createWebViewContentIntent(@NonNull Intent webViewIntent, @NonNull  String url) {
        webViewIntent.putExtra(WebViewActivity.EXTRA_URL, url);
        webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        webViewIntent.setClass(context, WebViewActivity.class);
        return webViewIntent;
    }

    /**
     * Creates content intent to start a browser for PendingIntent
     */
    @Nullable
    public Intent createBrowserIntent(@NonNull String browserUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) == null) return null;
        return intent;
    }

    @Nullable
    public Intent createBrowserIntent(@NonNull Message message) {
        return createBrowserIntent(message.getBrowserUrl());
    }
}
