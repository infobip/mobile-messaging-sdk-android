package org.infobip.mobile.messaging;

/**
 * @author mstipanov
 * @since 01.03.2016.
 */
public enum Event {
    REGISTRATION_ACQUIRED("org.infobip.mobile.messaging.gcm.REGISTRATION_ACQUIRED"),
    REGISTRATION_CREATED("org.infobip.mobile.messaging.gcm.REGISTRATION_CREATED"),
    REGISTRATION_CHANGED("org.infobip.mobile.messaging.gcm.REGISTRATION_CHANGED"),
    MESSAGE_RECEIVED("org.infobip.mobile.messaging.gcm.MESSAGE_RECEIVED"),
    API_COMMUNICATION_ERROR("org.infobip.mobile.messaging.infobip.API_COMMUNICATION_ERROR"),
    DELIVERY_REPORTS_SENT("org.infobip.mobile.messaging.infobip.DELIVERY_REPORTS_SENT");

    private final String key;

    Event(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
