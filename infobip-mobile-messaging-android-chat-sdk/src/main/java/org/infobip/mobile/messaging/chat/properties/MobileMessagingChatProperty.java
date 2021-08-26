package org.infobip.mobile.messaging.chat.properties;

public enum MobileMessagingChatProperty {

    ON_MESSAGE_TAP_ACTIVITY_CLASSES("org.infobip.mobile.messaging.infobip.chat.ON_MESSAGE_TAP_ACTIVITY_CLASSES", new Class[0]),
    IN_APP_CHAT_WIDGET_ID("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_WIDGET_ID", null),
    IN_APP_CHAT_WIDGET_TITLE("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_WIDGET_TITLE", null),
    IN_APP_CHAT_WIDGET_PRIMARY_COLOR("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_WIDGET_PRIMARY_COLOR", null),
    IN_APP_CHAT_WIDGET_BACKGROUND_COLOR("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR", null),
    IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE", null),
    IN_APP_CHAT_PERMISSION_FIRST_TIME_ASK("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_PERMISSION_FIRST_TIME_ASK", null),
    IN_APP_CHAT_ACTIVATED("org.infobip.mobile.messaging.infobip.IN_APP_CHAT_ACTIVATED", false),
    UNREAD_CHAT_MESSAGES_COUNT("org.infobip.mobile.messaging.infobip.UNREAD_CHAT_MESSAGES_COUNT", 0);

    private final String key;
    private final Object defaultValue;

    MobileMessagingChatProperty(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}

