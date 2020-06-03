package org.infobip.mobile.messaging.mobileapi.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiIOException;

/**
 * @author sslavin
 * @since 25/07/2017.
 */

public class BackendInvalidParameterException extends BackendBaseException {
    public BackendInvalidParameterException(String message, ApiIOException cause) {
        super(message, cause);
    }
}
