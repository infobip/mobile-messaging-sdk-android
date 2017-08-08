package org.infobip.mobile.messaging.interactive.platform;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public interface InteractiveBroadcaster {

    /**
     * Sends broadcast with tapped action for interactive category set on message.
     *
     * @param message              received message
     * @param notificationCategory tapped category
     * @param notificationAction   tapped action
     */
    void notificationActionTapped(Message message, NotificationCategory notificationCategory, NotificationAction notificationAction);
}
