package org.infobip.mobile.messaging.mobile;

public enum InternalSdkError {
    ERROR_SAVING_MESSAGE("20000", "Error saving message"),
    ERROR_ACCESSING_GCM("20001", "Error accessing GCM"),
    ERROR_TOKEN_REFRESH("20002", "Failed to complete token refresh"),
    ERROR_GCM_TOKEN_CLEANUP("20003", "Failed to complete GCM token cleanup"),
    ERROR_EMPTY_SYSTEM_DATA("20004", "System data is empty, cannot report"),
    DEVICE_NOT_SUPPORTED("20005", "Device is not supported"),
    NO_VALID_REGISTRATION("20006", "There is no valid registration");

    private final String code;
    private final String message;

    InternalSdkError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Creates new instance of {@link MobileMessagingError} and returns it's <i>toString()</i> method
     *
     * @return {@link MobileMessagingError#toString()} with specific error code and description message
     */
    public String get() {
        return new MobileMessagingError(code, message).toString();
    }

    public InternalSdkException getException() {
        return new InternalSdkException(new MobileMessagingError(code, message).toString());
    }

    private class InternalSdkException extends RuntimeException {
        InternalSdkException(String message) {
            super(message);
        }
    }
}
