package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public interface DialogStack {
    void add(InAppView view, Message message, NotificationCategory category, NotificationAction actions[]);
    void remove(InAppView view);
    void clear();
}
