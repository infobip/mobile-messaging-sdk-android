package org.infobip.mobile.messaging.api.support;

/**
 * @author sslavin
 * @since 26.04.2016.
 */
public class ApiInvalidParameterException extends ApiIOException {
    public ApiInvalidParameterException(String code, String message) {
        super(code, message);
    }

    public ApiInvalidParameterException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
