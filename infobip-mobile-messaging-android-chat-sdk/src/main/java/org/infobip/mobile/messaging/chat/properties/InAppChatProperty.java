package org.infobip.mobile.messaging.chat.properties;


public enum InAppChatProperty {

    ON_MESSAGE_TAP_ACTIVITY_CLASSES("org.infobip.mobile.messaging.infobip.chat.ON_MESSAGE_TAP_ACTIVITY_CLASSES", new Class[0]);

    private final String key;
    private final Object defaultValue;

    InAppChatProperty(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getKey() {
        return key;
    }
}
