package org.infobip.mobile.messaging.api.support;

/**
 * @author pandric on 10/04/2017.
 */

public enum CustomApiHeaders {
    FOREGROUND("foreground"),
    PUSH_REGISTRATION_ID("pushregistrationid");

    private final String value;

    CustomApiHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
