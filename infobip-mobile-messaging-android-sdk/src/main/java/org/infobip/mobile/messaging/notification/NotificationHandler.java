package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 30/05/2017.
 */

public interface NotificationHandler {

    /**
     * Displays native android notification for the provided message.
     * @param message message to display notification for.
     *
     * @return notification ID. -1 if notification wasn't displayed
     */
    int displayNotification(Message message);

    /**
     * Cancels all outstanding notifications.
     */
    void cancelAllNotifications();
}
