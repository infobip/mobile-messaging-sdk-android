package org.infobip.mobile.messaging.api.support;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class ApiBackendException extends ApiIOException {
    public ApiBackendException(String code, String message) {
        super(code, message);
    }

    public ApiBackendException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
