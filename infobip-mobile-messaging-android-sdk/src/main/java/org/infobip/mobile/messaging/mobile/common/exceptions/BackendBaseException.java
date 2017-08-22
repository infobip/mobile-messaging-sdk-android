package org.infobip.mobile.messaging.mobile.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

/**
 * @author sslavin
 * @since 26/07/2017.
 */

public abstract class BackendBaseException extends RuntimeException {

    public BackendBaseException(String message, ApiIOException cause) {
        super(message, cause);
    }

    /**
     * Creates error based on exception contents.
     * @return mobile messaging error.
     */
    public MobileMessagingError getError() {
        return MobileMessagingError.createFrom(getCause());
    }
}
