package org.infobip.mobile.messaging.tasks;

/**
 * @author mstipanov
 * @since 24.03.2016.
 */
public class RegisterMsisdnResult {
    private final String msisdn;

    public RegisterMsisdnResult(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getMsisdn() {
        return msisdn;
    }
}
