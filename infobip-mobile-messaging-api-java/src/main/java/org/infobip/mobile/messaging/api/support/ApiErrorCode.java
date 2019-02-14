package org.infobip.mobile.messaging.api.support;

/**
 * @author sslavin
 * @since 19/07/16.
 */
@SuppressWarnings("unused")
public class ApiErrorCode {
    public static final String UNKNOWN = "0";
    public static final String INVALID_APPLICATION_CODE = "1";
    public static final String INVALID_CLOUD_TYPE = "2";
    public static final String UNSUPPORTED_CLOUD_TYPE = "4";
    public static final String MESSAGE_IDS_EMPTY = "6";
    public static final String INVALID_INTERNAL_REGISTRATION_ID = "7";
    public static final String INVALID_VALUE = "9";

    public static final String EMAIL_INVALID = "EMAIL_INVALID";
    public static final String PHONE_INVALID = "PHONE_INVALID";
    public static final String USER_IDENTITY_INVALID = "USER_IDENTITY_INVALID";
    public static final String REQUEST_FORMAT_INVALID = "REQUEST_FORMAT_INVALID";
    public static final String USER_MERGE_INTERRUPTED = "USER_MERGE_INTERRUPTED";
    public static final String USER_DATA_RESTRICTED = "USER_DATA_RESTRICTED";
    public static final String PERSONALIZATION_IMPOSSIBLE = "PERSONALIZATION_IMPOSSIBLE";
    public static final String AMBIGUOUS_PERSONALIZE_CANDIDATES = "AMBIGUOUS_PERSONALIZE_CANDIDATES";
}
