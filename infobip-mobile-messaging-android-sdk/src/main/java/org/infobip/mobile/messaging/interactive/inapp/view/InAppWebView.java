package org.infobip.mobile.messaging.interactive.inapp.view;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;

public interface InAppWebView extends InAppView {
    void show(@NonNull InAppWebViewMessage message, @NonNull NotificationAction... actions);
}
