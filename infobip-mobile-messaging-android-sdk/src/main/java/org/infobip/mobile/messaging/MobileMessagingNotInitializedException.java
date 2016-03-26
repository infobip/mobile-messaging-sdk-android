package org.infobip.mobile.messaging;

/**
 * @author mstipanov
 * @since 01.03.2016.
 */
public class MobileMessagingNotInitializedException extends RuntimeException {
    public MobileMessagingNotInitializedException(String detailMessage) {
        super(detailMessage);
    }
}
