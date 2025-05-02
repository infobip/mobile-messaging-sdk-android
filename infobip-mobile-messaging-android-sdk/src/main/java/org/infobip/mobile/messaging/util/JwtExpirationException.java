package org.infobip.mobile.messaging.util;

public class JwtExpirationException extends RuntimeException {
    private static final String message = "The provided JWT is expired.";

    public JwtExpirationException() {
        super(message);
    }
}
