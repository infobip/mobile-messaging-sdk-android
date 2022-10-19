package org.infobip.mobile.messaging;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import java.util.UUID;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */

public enum MobileMessagingProperty {

    // START: prefs required for successfully connected Firebase registration with Push server
    API_URI("org.infobip.mobile.messaging.infobip.API_URI", "https://mobile.infobip.com/"),
    APPLICATION_CODE("org.infobip.mobile.messaging.infobip.APPLICATION_CODE", null, true),
    APPLICATION_CODE_HASH("org.infobip.mobile.messaging.infobip.APPLICATION_CODE_HASH", null, true),
    INFOBIP_REGISTRATION_ID("org.infobip.mobile.messaging.infobip.REGISTRATION_ID", null, true),
    CLOUD_TOKEN("org.infobip.mobile.messaging.gcm.REGISTRATION_ID", null, true),
    CLOUD_TOKEN_REPORTED("org.infobip.mobile.messaging.gcm.GCM_REGISTRATION_ID_REPORTED", false),
    REPORTED_PUSH_SERVICE_TYPE("org.infobip.mobile.messaging.REPORTED_PUSH_SERVICE_TYPE"),
    PERFORMED_USER_DATA_MIGRATION("org.infobip.mobile.messaging.PERFORMED_USER_DATA_MIGRATION"),
    USE_PRIVATE_SHARED_PREFS("org.infobip.mobile.messaging.infobip.USE_PRIVATE_SHARED_PREFS", true),
    // END

    // START: prefs required for keeping up-to-date state of MM SDK
    BATCH_REPORTING_DELAY("org.infobip.mobile.messaging.notification.BATCH_REPORTING_DELAY", 5000L),
    VERSION_CHECK_INTERVAL_DAYS("org.infobip.mobile.messaging.notification.VERSION_CHECK_INTERVAL_DAYS", 1),
    VERSION_CHECK_LAST_TIME("org.infobip.mobile.messaging.notification.VERSION_CHECK_LAST_TIME", 0L),
    BASEURL_CHECK_INTERVAL_HOURS("org.infobip.mobile.messaging.notification.BASEURL_CHECK_INTERVAL_HOURS", 24),
    BASEURL_CHECK_LAST_TIME("org.infobip.mobile.messaging.notification.BASEURL_CHECK_LAST_TIME", 0L),

    DEFAULT_MAX_RETRY_COUNT("org.infobip.mobile.messaging.infobip.DEFAULT_MAX_RETRY_COUNT", 3),
    DEFAULT_EXP_BACKOFF_MULTIPLIER("org.infobip.mobile.messaging.infobip.DEFAULT_EXP_BACKOFF_MULTIPLIER", 2),
    KEY_PERMISSION_REQUESTED_FIRST_TIME("org.infobip.mobile.messaging.infobip.PERMISSION_REQUESTED_FIRST_TIME", null),
    KEY_PERMISSIONS_SETTINGS_DIALOG_WAS_SHOWN("org.infobip.mobile.messaging.infobip.PERMISSIONS_SETTINGS_DIALOG_WAS_SHOWN", null),

    // END

    // START: MO/MT messages related prefs
    INFOBIP_UNREPORTED_MESSAGE_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_MESSAGE_IDS", new String[0]),
    INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS", new String[0]),
    INFOBIP_GENERATED_MESSAGE_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_GENERATED_MESSAGE_IDS", new String[0]),
    INFOBIP_SYNC_MESSAGES_IDS("org.infobip.mobile.messaging.infobip.INFOBIP_SYNC_MESSAGES_IDS", new String[0]),
    MESSAGE_STORE_CLASS("org.infobip.mobile.messaging.infobip.MESSAGE_STORE_CLASS"),
    UNSENT_MO_MESSAGES("org.infobip.mobile.messaging.infobip.UNSENT_MO_MESSAGES", new String[0]),
    // END

    // START: notifications config
    DISPLAY_NOTIFICATION_ENABLED("org.infobip.mobile.messaging.notification.DISPLAY_NOTIFICATION_ENABLED", true),
    CALLBACK_ACTIVITY("org.infobip.mobile.messaging.notification.CALLBACK_ACTIVITY"),
    DEFAULT_ICON("org.infobip.mobile.messaging.notification.DEFAULT_ICON", 0),
    DEFAULT_COLOR("org.infobip.mobile.messaging.notification.DEFAULT_COLOR", 0),
    DEFAULT_TITLE("org.infobip.mobile.messaging.notification.DEFAULT_TITLE", "Message"),
    INTENT_FLAGS("org.infobip.mobile.messaging.notification.INTENT_FLAGS", Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),

    PENDING_INTENT_FLAGS("org.infobip.mobile.messaging.notification.PENDING_INTENT_FLAGS", (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_CANCEL_CURRENT),

    NOTIFICATION_AUTO_CANCEL("org.infobip.mobile.messaging.notification.NOTIFICATION_AUTO_CANCEL", true),
    FOREGROUND_NOTIFICATION_ENABLED("org.infobip.mobile.messaging.notification.FOREGROUND_NOTIFICATION_ENABLED", true),
    MULTIPLE_NOTIFICATIONS_ENABLED("org.infobip.mobile.messaging.infobip.MULTIPLE_NOTIFICATIONS_ENABLED", false),
    HEADSUP_NOTIFICATIONS_ENABLED("org.infobip.mobile.messaging.infobip.HEADSUP_NOTIFICATIONS_ENABLED", true),
    MARK_SEEN_ON_NOTIFICATION_TAP("org.infobip.mobile.messaging.infobip.MARK_SEEN_ON_NOTIFICATION_TAP", true),
    INTERACTIVE_CATEGORIES("org.infobip.mobile.messaging.infobip.INTERACTIVE_CATEGORIES"),
    MODAL_IN_APP_NOTIFICATIONS_ENABLED("org.infobip.mobile.messaging.infobip.MODAL_IN_APP_NOTIFICATIONS_ENABLED", true),

    GEOFENCING_ACTIVATED("org.infobip.mobile.messaging.geo.GEOFENCING_ACTIVATED", false),
    // END

    // START: privacy settings prefs
    REPORT_CARRIER_INFO("org.infobip.mobile.messaging.infobip.REPORT_CARRIER_INFO", true),
    REPORT_SYSTEM_INFO("org.infobip.mobile.messaging.infobip.REPORT_SYSTEM_INFO", true),
    SAVE_USER_DATA_ON_DISK("org.infobip.mobile.messaging.infobip.SAVE_USER_DATA_ON_DISK", true),
    SAVE_APP_CODE_ON_DISK("org.infobip.mobile.messaging.infobip.SAVE_APP_CODE_ON_DISK", true),
    ALLOW_UNTRUSTED_SSL_ON_ERROR("org.infobip.mobile.messaging.infobip.ALLOW_UNTRUSTED_SSL_ON_ERROR", false),
    APP_CODE_PROVIDER_CANONICAL_CLASS_NAME("org.infobip.mobile.messaging.infobip.APP_CODE_PROVIDER_CANONICAL_CLASS_NAME"),
    // END

    // START: installation (primary, app user ID, system, ...) and user related prefs
    UNIVERSAL_INSTALLATION_ID("org.infobip.mobile.messaging.infobip.UNIVERSAL_INSTALLATION_ID", UUID.randomUUID().toString()),

    MOBILE_CARRIER_NAME("org.infobip.mobile.messaging.infobip.MOBILE_CARRIER_NAME", ""),
    MOBILE_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.MCC", ""),
    MOBILE_NETWORK_CODE("org.infobip.mobile.messaging.infobip.MNC", ""),
    SIM_CARRIER_NAME("org.infobip.mobile.messaging.infobip.SIM_CARRIER_NAME", ""),
    SIM_COUNTRY_CODE("org.infobip.mobile.messaging.infobip.SIM_MCC", ""),
    SIM_NETWORK_CODE("org.infobip.mobile.messaging.infobip.SIM_MNC", ""),

    UNREPORTED_SYSTEM_DATA("org.infobip.mobile.messaging.infobip.UNREPORTED_SYSTEM_DATA"),
    REPORTED_SYSTEM_DATA_HASH("org.infobip.mobile.messaging.infobip.REPORTED_SYSTEM_DATA_HASH", 0),
    SYSTEM_DATA_VERSION_POSTFIX("org.infobip.mobile.messaging.SYSTEM_DATA_VERSION_POSTFIX"),

    IS_PRIMARY("org.infobip.mobile.messaging.infobip.IS_PRIMARY", false),
    IS_PRIMARY_UNREPORTED("org.infobip.mobile.messaging.infobip.IS_PRIMARY_UNREPORTED"),
    APP_USER_ID("org.infobip.mobile.messaging.infobip.APP_USER_ID"),
    IS_APP_USER_ID_UNREPORTED("org.infobip.mobile.messaging.infobip.IS_APP_USER_ID_UNREPORTED", false),
    CUSTOM_ATTRIBUTES("org.infobip.mobile.messaging.infobip.CUSTOM_ATTRIBUTES"),
    UNREPORTED_CUSTOM_ATTRIBUTES("org.infobip.mobile.messaging.infobip.UNREPORTED_CUSTOM_ATTRIBUTES"),
    PUSH_REGISTRATION_ENABLED("org.infobip.mobile.messaging.infobip.PUSH_REGISTRATION_ENABLED", true),
    UNREPORTED_PUSH_REGISTRATION_ENABLED("org.infobip.mobile.messaging.infobip.UNREPORTED_PUSH_REGISTRATION_ENABLED"),

    IS_DEPERSONALIZE_UNREPORTED("org.infobip.mobile.messaging.infobip.IS_DEPERSONALIZE_UNREPORTED", false),
    SHOULD_REPERSONALIZE("org.infobip.mobile.messaging.infobip.SHOULD_REPERSONALIZE", false),

    UNREPORTED_USER_DATA("org.infobip.mobile.messaging.infobip.UNREPORTED_USER_DATA"),
    USER_DATA("org.infobip.mobile.messaging.infobip.USER_DATA"),
    USER_INSTALLATIONS_EXPIRE_AT("org.infobip.mobile.messaging.infobip.USER_INSTALLATIONS_EXPIRE_AT"),

    USER_CUSTOM_EVENTS("org.infobip.mobile.messaging.infobip.USER_CUSTOM_EVENTS"),
    LAST_REPORTED_ACTIVE_SESSION_START_TIME_MILLIS("org.infobip.mobile.messaging.infobip.LAST_REPORTED_ACTIVE_SESSION_START_TIME_MILLIS", 0L),
    ACTIVE_SESSION_START_TIME_MILLIS("org.infobip.mobile.messaging.infobip.ACTIVE_SESSION_START_TIME_MILLIS", 0L),
    ACTIVE_SESSION_END_TIME_MILLIS("org.infobip.mobile.messaging.infobip.ACTIVE_SESSION_END_TIME_MILLIS", 0L),
    SESSION_BOUNDS("org.infobip.mobile.messaging.infobip.SESSION_BOUNDS");
    // END

    private final String key;
    private final Object defaultValue;
    private final boolean encrypted;

    MobileMessagingProperty(String key) {
        this(key, null, false);
    }

    MobileMessagingProperty(String key, Object defaultValue) {
        this(key, defaultValue, false);
    }

    MobileMessagingProperty(String key, Object defaultValue, boolean encrypted) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.encrypted = encrypted;
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isEncrypted() {
        return encrypted;
    }
}
