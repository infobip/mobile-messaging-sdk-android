package org.infobip.mobile.messaging.interactive.inapp.view.ctx;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppCtxVisitor;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppNativeView;

public class InAppNativeCtx implements InAppCtx {
    private final InAppNativeView inAppView;
    private final Message message;
    private final NotificationCategory category;
    private final NotificationAction[] actions;

    public InAppNativeCtx(InAppNativeView inAppView, Message message, NotificationCategory category, NotificationAction[] actions) {
        this.inAppView = inAppView;
        this.message = message;
        this.category = category;
        this.actions = actions;
    }

    public Message getMessage() {
        return message;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public NotificationAction[] getActions() {
        return actions;
    }

    public InAppNativeView getInAppView() {
        return inAppView;
    }

    public void show() {
        inAppView.show(message, category, actions);
    }

    public void accept(InAppCtxVisitor visitor) {
        visitor.visit(this);
    }
}

