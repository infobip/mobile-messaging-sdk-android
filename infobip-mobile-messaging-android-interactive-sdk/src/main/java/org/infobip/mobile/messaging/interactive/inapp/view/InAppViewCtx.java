package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;

/**
 * @author sslavin
 * @since 25/04/2018.
 */

public class InAppViewCtx {
    private final InAppView inAppView;
    private final Message message;
    private final NotificationAction actions[];

    InAppViewCtx(InAppView inAppView, Message message, NotificationAction[] actions) {
        this.inAppView = inAppView;
        this.message = message;
        this.actions = actions;
    }

    public InAppView getInAppView() {
        return inAppView;
    }

    public Message getMessage() {
        return message;
    }

    public NotificationAction[] getActions() {
        return actions;
    }
}
