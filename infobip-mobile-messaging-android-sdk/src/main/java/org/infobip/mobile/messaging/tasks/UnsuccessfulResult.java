package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiException;

/**
 * @author sslavin
 * @since 19/07/16.
 */
public class UnsuccessfulResult {
    private final Throwable exception;

    public UnsuccessfulResult() {
        exception = null;
    }

    public UnsuccessfulResult(Throwable exception) {
        this.exception = exception;
    }

    public Throwable getError() {
        return exception;
    }

    public boolean hasError() {
        return exception != null;
    }

    public boolean hasInvalidParameterError() {
        if (exception == null || !(exception instanceof ApiException)) {
            return false;
        }

        String code = ((ApiException) exception).getCode();
        return ApiErrorCode.INVALID_MSISDN_FORMAT.equals(code) ||
                ApiErrorCode.INVALID_VALUE.equals(code);

    }
}
