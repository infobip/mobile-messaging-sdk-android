package org.infobip.mobile.messaging.mobile.common.exceptions;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;

/**
 * @author sslavin
 * @since 17/10/2017.
 */

public class BackendCommunicationExceptionWithContent extends BackendBaseExceptionWithContent {
    public BackendCommunicationExceptionWithContent(String message, ApiBackendExceptionWithContent cause) {
        super(message, cause);
    }
}
