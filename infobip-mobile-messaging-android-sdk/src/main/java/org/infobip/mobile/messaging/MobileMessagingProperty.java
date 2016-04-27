package org.infobip.mobile.messaging;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public enum MobileMessagingProperty {

    GCM_REGISTRATION_ID_SAVED("org.infobip.mobile.messaging.gcm.GCM_REGISTRATION_ID_SAVED", false),
    GCM_REGISTRATION_ID("org.infobip.mobile.messaging.gcm.REGISTRATION_ID"),
    INFOBIP_REGISTRATION_ID("org.infobip.mobile.messaging.infobip.REGISTRATION_ID"),
    GCM_SENDER_ID("org.infobip.mobile.messaging.gcm.GCM_SENDER_ID"),
    APPLICATION_CODE("org.infobip.mobile.messaging.infobip.APPLICATION_CODE"),
    LAST_HTTP_EXCEPTION("org.infobip.mobile.messaging.infobip.LAST_HTTP_EXCEPTION"),
    INFOBIP_UNREPORTED_MESSAGE_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_MESSAGE_IDS", new String[0]),
    INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS", new String[0]),
    API_URI("org.infobip.mobile.messaging.infobip.API_URI", "https://oneapi.infobip.com/"),
    MESSAGE_STORE_CLASS("org.infobip.mobile.messaging.infobip.MESSAGE_STORE_CLASS"),
    MSISDN("org.infobip.mobile.messaging.infobip.MSISDN"),
    MSISDN_SAVED("org.infobip.mobile.messaging.infobip.MSISDN_SAVED"),
    MOBILE_CARRIER_NAME("org.infobip.mobile.messaging.infobip.MOBILE_CARRIER_NAME", ""),
    MOBILE_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.MCC", ""),
    MOBILE_NETWORK_CODE("org.infobip.mobile.messaging.infobip.MNC", ""),
    SIM_CARRIER_NAME("org.infobip.mobile.messaging.infobip.SIM_CARRIER_NAME", ""),
    SIM_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.SIM_MCC", ""),
    SIM_NETWORK_CODE("org.infobip.mobile.messaging.infobip.SIM_MNC", ""),

    DISPLAY_NOTIFICATION_ENABLED("org.infobip.mobile.messaging.notification.DISPLAY_NOTIFICATION_ENABLED", true),
    CALLBACK_ACTIVITY("org.infobip.mobile.messaging.notification.CALLBACK_ACTIVITY"),
    DEFAULT_ICON("org.infobip.mobile.messaging.notification.DEFAULT_ICON", 0),
    DEFAULT_TITLE("org.infobip.mobile.messaging.notification.DEFAULT_TITLE", "Message"),
    INTENT_FLAGS("org.infobip.mobile.messaging.notification.INTENT_FLAGS", Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PENDING_INTENT_FLAGS("org.infobip.mobile.messaging.notification.PENDING_INTENT_FLAGS", PendingIntent.FLAG_CANCEL_CURRENT),
    NOTIFICATION_DEFAULTS("org.infobip.mobile.messaging.notification.NOTIFICATION_DEFAULTS", Notification.DEFAULT_ALL),
    NOTIFICATION_AUTO_CANCEL("org.infobip.mobile.messaging.notification.NOTIFICATION_AUTO_CANCEL", true);

    private final String key;
    private final Object defaultValue;

    MobileMessagingProperty(String key) {
        this(key, null);
    }

    MobileMessagingProperty(String key, Object defaultValue) {
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
