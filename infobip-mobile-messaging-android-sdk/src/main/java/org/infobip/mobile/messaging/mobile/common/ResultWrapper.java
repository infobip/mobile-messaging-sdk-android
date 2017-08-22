package org.infobip.mobile.messaging.mobile.common;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

class ResultWrapper<IN, OUT> {
    IN inputs[];
    OUT result;
    Throwable error;

    ResultWrapper(OUT result) {
        this.result = result;
    }

    ResultWrapper(IN inputs[], Throwable error) {
        this.inputs = inputs;
        this.error = error;
    }
}
