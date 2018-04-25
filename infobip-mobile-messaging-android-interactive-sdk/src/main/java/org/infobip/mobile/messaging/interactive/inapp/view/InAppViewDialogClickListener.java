package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;

/**
 * @author sslavin
 * @since 12/04/2018.
 */
public class InAppViewDialogClickListener implements DialogInterface.OnClickListener {
    private final InAppView inAppView;
    private final InAppView.Callback callback;
    private final Message message;
    private final NotificationAction action;

    InAppViewDialogClickListener(InAppView inAppView, InAppView.Callback callback, Message message, NotificationAction action) {
        this.inAppView = inAppView;
        this.callback = callback;
        this.message = message;
        this.action = action;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        callback.buttonPressedFor(inAppView, message, action);
    }
}
