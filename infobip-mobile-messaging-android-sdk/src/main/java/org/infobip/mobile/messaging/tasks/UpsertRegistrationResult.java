package org.infobip.mobile.messaging.tasks;

/**
 * @author sslavin
 * @since 19/07/16.
 */
public class UpsertRegistrationResult extends UnsuccessfulResult {
    private final String deviceInstanceId;

    public UpsertRegistrationResult(String deviceInstanceId) {
        this.deviceInstanceId = deviceInstanceId;
    }

    public UpsertRegistrationResult(Throwable exception) {
        super(exception);
        deviceInstanceId = null;
    }

    public String getDeviceInstanceId() {
        return deviceInstanceId;
    }
}
