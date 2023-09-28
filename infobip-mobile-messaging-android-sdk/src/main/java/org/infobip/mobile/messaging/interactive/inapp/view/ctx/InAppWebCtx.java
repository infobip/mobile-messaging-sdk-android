package org.infobip.mobile.messaging.interactive.inapp.view.ctx;

import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppCtxVisitor;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppWebView;

public class InAppWebCtx implements InAppCtx {
    private final InAppWebView inAppView;
    private final InAppWebViewMessage inAppWebViewMessage;
    private final NotificationAction[] actions;

    public InAppWebCtx(InAppWebView inAppView, InAppWebViewMessage inAppWebViewMessage, NotificationAction[] actions) {
        this.inAppView = inAppView;
        this.inAppWebViewMessage = inAppWebViewMessage;
        this.actions = actions;
    }

    public void accept(InAppCtxVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public InAppWebView getInAppView() {
        return inAppView;
    }

    public InAppWebViewMessage getInAppWebViewMessage() {
        return inAppWebViewMessage;
    }

    public void show() {
        inAppView.show(inAppWebViewMessage, actions);
    }
}
