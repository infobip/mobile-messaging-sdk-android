package org.infobip.mobile.messaging.api.support;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class NoHttpRequestAnnotation extends RuntimeException {
    public NoHttpRequestAnnotation(String message) {
        super(message);
    }

    public NoHttpRequestAnnotation(String message, Throwable cause) {
        super(message, cause);
    }
}
