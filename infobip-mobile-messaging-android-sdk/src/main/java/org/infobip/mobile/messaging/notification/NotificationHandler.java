package org.infobip.mobile.messaging.notification;

import android.content.Context;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 30/05/2017.
 */

public interface NotificationHandler {

    void setContext(Context context);

    /**
     * Displays native android notification for the provided message.
     * @param message message to display notification for.
     */
    void displayNotification(Message message);

    /**
     * Cancels all outstanding notifications.
     */
    void cancelAllNotifications();
}
