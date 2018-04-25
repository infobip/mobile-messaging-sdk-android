package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public interface DialogStack {
    void add(InAppView view, Message message, NotificationAction actions[]);
    void remove(InAppView view);
    void clear();
}
