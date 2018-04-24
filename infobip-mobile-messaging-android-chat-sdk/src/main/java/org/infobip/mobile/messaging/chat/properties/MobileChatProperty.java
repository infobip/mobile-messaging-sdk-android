package org.infobip.mobile.messaging.chat.properties;

/**
 * @author sslavin
 * @since 01/11/2017.
 */

public enum MobileChatProperty {
    USER_NAME_DIALOG_SHOWN("org.infobip.mobile.messaging.infobip.chat.USER_NAME_DIALOG_SHOWN", false),
    ON_MESSAGE_TAP_ACTIVITY_CLASSES("org.infobip.mobile.messaging.infobip.chat.ON_MESSAGE_TAP_ACTIVITY_CLASSES", new Class[0]);

    private final String key;
    private final Object defaultValue;

    MobileChatProperty(String key, Object defaultValue) {
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
