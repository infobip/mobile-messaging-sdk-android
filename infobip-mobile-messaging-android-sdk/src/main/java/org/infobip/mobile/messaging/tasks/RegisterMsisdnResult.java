package org.infobip.mobile.messaging.tasks;

/**
 * @author mstipanov
 * @since 24.03.2016.
 */
public class RegisterMsisdnResult {
    private final long msisdn;

    public RegisterMsisdnResult(long msisdn) {
        this.msisdn = msisdn;
    }

    public long getMsisdn() {
        return msisdn;
    }
}
