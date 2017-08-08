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
    NOTIFICATION_ACTION_TAPPED("org.infobip.mobile.messaging.interactive.NOTIFICATION_ACTION_TAPPED");

    private final String key;

    InteractiveEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
