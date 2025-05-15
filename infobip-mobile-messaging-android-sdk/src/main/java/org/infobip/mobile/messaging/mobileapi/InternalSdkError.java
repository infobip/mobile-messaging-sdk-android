package org.infobip.mobile.messaging.mobileapi;

public enum InternalSdkError {
    ERROR_SAVING_MESSAGE("20000", "Error saving message"),
    ERROR_ACCESSING_PLAY_SERVICES("20001", "Error accessing play services, use provided 'googleErrorCode' with 'GoogleApiAvailability.getErrorDialog()' (https://developers.google.com/android/reference/com/google/android/gms/common/GoogleApiAvailability.html)"),
    ERROR_TOKEN_REFRESH("20002", "Failed to complete token refresh"),
    ERROR_GCM_TOKEN_CLEANUP("20003", "Failed to complete GCM token cleanup"),
    ERROR_EMPTY_SYSTEM_DATA("20004", "System data is empty, cannot report"),
    DEVICE_NOT_SUPPORTED("20005", "Device is not supported"),
    NO_VALID_REGISTRATION("20006", "There is no valid registration"),
    DEPERSONALIZATION_IN_PROGRESS("20007", "Depersonalization is currently in progress"),
    ERROR_SAVING_EMPTY_OBJECT("20008", "Cannot save object without changes"),
    NETWORK_UNAVAILABLE("20009", "Network unavailable"),
    ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED("20010", "Maximum allowed attachment size exceeded"),
    INSTALLATION_SYNC_IN_PROGRESS("20011", "Installation sync is already in progress"),
    ERROR_ATTACHMENT_NOT_VALID("20012", "Attachment is not valid"),
    JWT_TOKEN_STRUCTURE_INVALID("20013", "JWT token structure invalid"),
    JWT_TOKEN_EXPIRED("20014", "JWT token is expired");

    private final String code;
    private final String message;

    InternalSdkError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Creates new instance of {@link MobileMessagingError} and returns its <i>toString()</i> method
     *
     * @return {@link MobileMessagingError#toString()} with specific error code and description message
     */
    public String get() {
        return new MobileMessagingError(code, message).toString();
    }

    public InternalSdkException getException() {
        return new InternalSdkException(getError().toString());
    }

    public MobileMessagingError getError() {
        return new MobileMessagingError(code, message);
    }

    public MobileMessagingError getError(String message) {
        return new MobileMessagingError(code, message);
    }

    public static class InternalSdkException extends RuntimeException {
        InternalSdkException(String message) {
            super(message);
        }
    }
}
