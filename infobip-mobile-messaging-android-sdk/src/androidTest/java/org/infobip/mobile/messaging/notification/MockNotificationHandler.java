package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.Message;

/**
 * @author tjuric
 * @since 19/09/17.
 */

public class MockNotificationHandler implements NotificationHandler {

    @Override
    public int displayNotification(Message message) {
        return 12345;
    }

    @Override
    public void cancelAllNotifications() {

    }
}
