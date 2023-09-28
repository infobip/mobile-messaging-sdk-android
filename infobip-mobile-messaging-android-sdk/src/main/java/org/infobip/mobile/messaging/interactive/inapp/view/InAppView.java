package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.Context;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author sslavin
 * @since 13/04/2018.
 */
public interface InAppView {
    interface Callback {
        void buttonPressedFor(@NonNull InAppNativeView inAppView, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction action);

        void actionButtonPressedFor(@NonNull InAppWebView inAppView, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction action);

        void notificationPressedFor(@NonNull InAppWebView inAppView, @NonNull Message message, @NonNull NotificationAction action, Context context);

        void dismissed(@NonNull InAppView inAppView);
    }
}

