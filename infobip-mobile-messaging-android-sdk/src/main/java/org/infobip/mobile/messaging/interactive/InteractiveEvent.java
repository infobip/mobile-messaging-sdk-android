/*
 * InteractiveEvent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive;


public enum InteractiveEvent {

    /**
     * It is triggered when notification action button is clicked/tapped.
     * <pre>
     * Message message = Message.createFrom(intent.getExtras());
     * NotificationAction notificationAction = NotificationAction.createFrom(intent.getExtras());
     * NotificationCategory notificationCategory = NotificationCategory.createFrom(intent.getExtras());
     * </pre>
     */
    NOTIFICATION_ACTION_TAPPED("org.infobip.mobile.messaging.interactive.NOTIFICATION_ACTION_TAPPED"),

    /**
     * It is triggered when in-app notification is ready to be shown.
     * <p>
     * Contains message.
     * <pre>
     * {@code
     * Message message = Message.createFrom(intent.getExtras());
     * }
     * </pre>
     */
    MODAL_IN_APP_NOTIFICATION_IS_READY_TO_DISPLAY("org.infobip.mobile.messaging.interactive.MODAL_IN_APP_NOTIFICATION_IS_READY_TO_DISPLAY");

    private final String key;

    InteractiveEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
