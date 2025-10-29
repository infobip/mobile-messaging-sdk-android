/*
 * InteractiveBroadcaster.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.platform;

import android.content.Intent;

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
    Intent notificationActionTapped(Message message, NotificationCategory notificationCategory, NotificationAction notificationAction);

    /**
     * Sends broadcast that in-app notification is ready to be displayed
     * @param message to display as in-app notification
     */
    void inAppNotificationIsReadyToDisplay(Message message);
}
