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
    public static final String INVALID_MSISDN_FORMAT = "5";
    public static final String MESSAGE_IDS_EMPTY = "6";
    public static final String INVALID_INTERNAL_REGISTRATION_ID = "7";
    public static final String CONTACT_NOT_FOUND = "8";
    public static final String INVALID_VALUE = "9";
    public static final String CONTACT_SERVICE_ERROR = "10";
    public static final String INVALID_EMAIL_FORMAT = "17";
    public static final String INVALID_BIRTHDATE_FORMAT = "18";

    public static final String USER_MERGE_INTERRUPTED = "USER_MERGE_INTERRUPTED"; //TODO change/agree on unified error contract
    public static final String USER_DATA_RESTRICTED = "USER_DATA_RESTRICTED"; //TODO change/agree on unified error contract
    //TODO revise all of these
}
