package org.infobip.mobile.messaging.mobile;

public enum InternalError {
    SAVE_MESSAGE_ERROR("0", "Error saving message"),
    ERROR_ACCESSING_GCM("1", "Error accessing GCM"),
    DEVICE_NOT_SUPPORTED("2", "Device is not supported"),
    TOKEN_REFRESH_ERROR("3", "Failed to complete token refresh"),
    GCM_TOKEN_CLEANUP_ERROR("4", "Failed to complete GCM token cleanup"),
    EMPTY_SYSTEM_DATA_ERROR("5", "System data is empty, cannot report"),
    NO_VALID_REGISTRATION("6", "There is no valid registration");

    private final String code;
    private final String message;

    InternalError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String get() {
        return new MobileMessagingError(code, message).toString();
    }
}
