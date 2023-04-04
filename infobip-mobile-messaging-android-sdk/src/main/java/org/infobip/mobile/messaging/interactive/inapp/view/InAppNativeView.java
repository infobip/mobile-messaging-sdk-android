package org.infobip.mobile.messaging.interactive.inapp.view;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

public interface InAppNativeView extends InAppView {
    void show(@NonNull Message message, NotificationCategory category, @NonNull NotificationAction... actions);
    void showWithImage(@NonNull Bitmap bitmap, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction... actions);
}
