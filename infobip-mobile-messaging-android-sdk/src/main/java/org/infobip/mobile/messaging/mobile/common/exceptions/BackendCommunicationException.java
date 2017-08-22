package org.infobip.mobile.messaging.mobile.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiIOException;

/**
 * @author sslavin
 * @since 25/07/2017.
 */

public class BackendCommunicationException extends BackendBaseException {
    public BackendCommunicationException(String message, ApiIOException cause) {
        super(message, cause);
    }
}
