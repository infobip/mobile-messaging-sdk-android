/*
 * InAppNotificationHandler.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
