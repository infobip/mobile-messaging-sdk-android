package org.infobip.mobile.messaging.api.support.http.client;

/**
 * @author sslavin
 * @since 27/11/2017.
 */

public interface RequestInterceptor {
    Request intercept(Request request);
}
