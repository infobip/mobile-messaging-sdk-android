package org.infobip.mobile.messaging.mobile.registration;

import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 19/07/16.
 */
class UpsertRegistrationResult extends UnsuccessfulResult {
    private final String deviceInstanceId;
    private final Boolean pushRegistrationEnabled;

    UpsertRegistrationResult(String deviceInstanceId, Boolean pushRegistrationEnabled) {
        super(null);
        this.deviceInstanceId = deviceInstanceId;
        this.pushRegistrationEnabled = pushRegistrationEnabled;
    }

    UpsertRegistrationResult(Throwable exception) {
        super(exception);
        deviceInstanceId = null;
        pushRegistrationEnabled = true;
    }

    String getDeviceInstanceId() {
        return deviceInstanceId;
    }

    Boolean getPushRegistrationEnabled() {
        return pushRegistrationEnabled;
    }
}
