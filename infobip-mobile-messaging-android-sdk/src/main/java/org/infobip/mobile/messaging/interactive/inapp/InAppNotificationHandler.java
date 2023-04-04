package org.infobip.mobile.messaging.interactive.inapp;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 16/04/2018.
 */
public interface InAppNotificationHandler {
    void handleMessage(Message message);
    void handleMessage(InAppWebViewMessage message);
    void userPressedNotificationButtonForMessage(Message message);
    void userTappedNotificationForMessage(Message message);
    void appWentToForeground();
    void displayDialogFor(Message message);
}
