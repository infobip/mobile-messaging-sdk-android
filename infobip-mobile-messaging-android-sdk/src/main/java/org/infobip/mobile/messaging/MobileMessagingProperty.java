package org.infobip.mobile.messaging;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public enum MobileMessagingProperty {

    GCM_REGISTRATION_ID_REPORTED("org.infobip.mobile.messaging.gcm.GCM_REGISTRATION_ID_REPORTED", false),
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
    MSISDN_TO_REPORT("org.infobip.mobile.messaging.infobip.MSISDN_TO_REPORT"),
    MOBILE_CARRIER_NAME("org.infobip.mobile.messaging.infobip.MOBILE_CARRIER_NAME", ""),
    MOBILE_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.MCC", ""),
    MOBILE_NETWORK_CODE("org.infobip.mobile.messaging.infobip.MNC", ""),
    SIM_CARRIER_NAME("org.infobip.mobile.messaging.infobip.SIM_CARRIER_NAME", ""),
    SIM_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.SIM_MCC", ""),
    SIM_NETWORK_CODE("org.infobip.mobile.messaging.infobip.SIM_MNC", ""),
    REPORT_CARRIER_INFO("org.infobip.mobile.messaging.infobip.REPORT_CARRIER_INFO", true),
    REPORT_SYSTEM_INFO("org.infobip.mobile.messaging.infobip.REPORT_SYSTEM_INFO", true),

    GEOFENCING_ACTIVATED("org.infobip.mobile.messaging.infobip.GEOFENCING_ACTIVATED", true),

    DISPLAY_NOTIFICATION_ENABLED("org.infobip.mobile.messaging.notification.DISPLAY_NOTIFICATION_ENABLED", true),
    CALLBACK_ACTIVITY("org.infobip.mobile.messaging.notification.CALLBACK_ACTIVITY"),
    DEFAULT_ICON("org.infobip.mobile.messaging.notification.DEFAULT_ICON", 0),
    DEFAULT_TITLE("org.infobip.mobile.messaging.notification.DEFAULT_TITLE", "Message"),
    INTENT_FLAGS("org.infobip.mobile.messaging.notification.INTENT_FLAGS", Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PENDING_INTENT_FLAGS("org.infobip.mobile.messaging.notification.PENDING_INTENT_FLAGS", PendingIntent.FLAG_CANCEL_CURRENT),
    NOTIFICATION_DEFAULTS("org.infobip.mobile.messaging.notification.NOTIFICATION_DEFAULTS", Notification.DEFAULT_ALL),
    NOTIFICATION_AUTO_CANCEL("org.infobip.mobile.messaging.notification.NOTIFICATION_AUTO_CANCEL", true),

    BATCH_REPORTING_DELAY("org.infobip.mobile.messaging.notification.BATCH_REPORTING_DELAY", 3000L),
    FOREGROUND_NOTIFICATION_ENABLED("org.infobip.mobile.messaging.notification.FOREGROUND_NOTIFICATION_ENABLED", true),

    EXTRA_MESSAGE("org.infobip.mobile.messaging.infobip.EXTRA_MESSAGE"),

    NOTIFICATION_MARK_SEEN_TITLE("org.infobip.mobile.messaging.notification.NOTIFICATION_MARK_SEEN_TITLE"),
    NOTIFICATION_REPLY_TITLE("org.infobip.mobile.messaging.notification.NOTIFICATION_REPLY_TITLE"),
    NOTIFICATION_OPEN_URL_TITLE("org.infobip.mobile.messaging.notification.NOTIFICATION_OPEN_URL_TITLE");

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
