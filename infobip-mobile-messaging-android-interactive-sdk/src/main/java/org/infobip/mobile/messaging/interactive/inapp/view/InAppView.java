package org.infobip.mobile.messaging.interactive.inapp.view;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author sslavin
 * @since 13/04/2018.
 */
public interface InAppView {
    void show(@NonNull Message message, @NonNull NotificationCategory category, @NonNull NotificationAction... actions);
    void showImage(@NonNull Bitmap bitmap);
    void imageDownloadFailed();

    interface Callback {
        void buttonPressedFor(@NonNull InAppView inAppView, @NonNull Message message, @NonNull NotificationCategory category, @NonNull NotificationAction action);
        void dismissed(@NonNull InAppView inAppView);
    }
}
