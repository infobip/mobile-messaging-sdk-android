package org.infobip.mobile.messaging.mobile.registration;

import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 19/07/16.
 */
class UpsertRegistrationResult extends UnsuccessfulResult {
    private final String deviceInstanceId;

    UpsertRegistrationResult(String deviceInstanceId) {
        super(null);
        this.deviceInstanceId = deviceInstanceId;
    }

    UpsertRegistrationResult(Throwable exception) {
        super(exception);
        deviceInstanceId = null;
    }

    String getDeviceInstanceId() {
        return deviceInstanceId;
    }
}
