package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceAtts;

import java.util.Map;

import static org.infobip.mobile.messaging.InstallationMapper.fromBundle;

public class Installation extends CustomAttributeHolder {

    private String pushRegistrationId;
    private Boolean isPrimaryDevice;
    private Boolean isPushRegistrationEnabled;
    private Boolean notificationsEnabled;
    private Boolean geoEnabled;
    private String sdkVersion;
    private String appVersion;
    private String os;
    private String osVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private Boolean deviceSecure;
    private String language;
    private String deviceTimezoneOffset;
    private String applicationUserId;
    private String deviceName;
    private PushServiceType pushServiceType;
    private String pushServiceToken;

    public enum PushServiceType {
        GCM,
        Firebase
    }

    public Installation() {

    }

    public Installation(String pushRegistrationId) {
        this.pushRegistrationId = pushRegistrationId;
    }

    public Installation(String pushRegistrationId,
                        Boolean isPushRegistrationEnabled,
                        Boolean notificationsEnabled,
                        Boolean geoEnabled,
                        String sdkVersion,
                        String appVersion,
                        String os,
                        String osVersion,
                        String deviceManufacturer,
                        String deviceModel,
                        Boolean deviceSecure,
                        String language,
                        String deviceTimezoneOffset,
                        String applicationUserId,
                        String deviceName,
                        Boolean isPrimaryDevice,
                        PushServiceType pushServiceType,
                        String pushServiceToken,
                        Map<String, CustomAttributeValue> customAttributes) {
        super(customAttributes);
        this.pushRegistrationId = pushRegistrationId;
        this.isPushRegistrationEnabled = isPushRegistrationEnabled;
        this.notificationsEnabled = notificationsEnabled;
        this.geoEnabled = geoEnabled;
        this.sdkVersion = sdkVersion;
        this.appVersion = appVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.deviceSecure = deviceSecure;
        this.language = language;
        this.deviceTimezoneOffset = deviceTimezoneOffset;
        this.applicationUserId = applicationUserId;
        this.deviceName = deviceName;
        this.isPrimaryDevice = isPrimaryDevice;
        this.pushServiceType = pushServiceType;
        this.pushServiceToken = pushServiceToken;
    }

    public static Installation createFrom(Bundle bundle) {
        return fromBundle(BroadcastParameter.EXTRA_INSTALLATION, bundle);
    }

    public String getPushRegistrationId() {
        return pushRegistrationId;
    }

    protected void setPushRegistrationId(String pushRegistrationId) {
        this.pushRegistrationId = pushRegistrationId;
        setField(AppInstanceAtts.pushRegId, pushRegistrationId);
    }

    /**
     * Use this method to determine if this device is currently primary device or not.
     *
     * @return <i>true</i> if this device is primary or false otherwise.
     */
    public Boolean isPrimaryDevice() {
        return isPrimaryDevice;
    }

    /**
     * This method allows you to configure this device as primary among other devices of a single user.
     */
    public void setPrimaryDevice(Boolean primaryDevice) {
        isPrimaryDevice = primaryDevice;
        setField(AppInstanceAtts.isPrimary, primaryDevice);
    }

    /**
     * Push registration status defines whether the device is allowed to receive push notifications from Infobip
     * (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     *
     * @return Current push registration status.
     */
    public Boolean isPushRegistrationEnabled() {
        return isPushRegistrationEnabled;
    }

    /**
     * Enables or disables the push registration. Installation is able to receive push notifications
     * through MobileMessaging SDK (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     *
     * @param pushRegistrationEnabled set to <i>true</i> to enable receiving of push notifications (regular push messages/geofencing
     *                                campaign messages/messages fetched from the server) over Infobip, <i>false</i> to disable it
     */
    public void setPushRegistrationEnabled(Boolean pushRegistrationEnabled) {
        isPushRegistrationEnabled = pushRegistrationEnabled;
        setField(AppInstanceAtts.regEnabled, pushRegistrationEnabled);
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
        setField(AppInstanceAtts.notificationsEnabled, notificationsEnabled);
    }

    public Boolean getGeoEnabled() {
        return geoEnabled;
    }

    public void setGeoEnabled(Boolean geoEnabled) {
        this.geoEnabled = geoEnabled;
        setField(AppInstanceAtts.geoEnabled, geoEnabled);
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
        setField(AppInstanceAtts.sdkVersion, sdkVersion);
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        setField(AppInstanceAtts.appVersion, appVersion);
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
        setField(AppInstanceAtts.os, os);
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
        setField(AppInstanceAtts.osVersion, osVersion);
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
        setField(AppInstanceAtts.deviceManufacturer, deviceManufacturer);
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
        setField(AppInstanceAtts.deviceModel, deviceModel);
    }

    public Boolean getDeviceSecure() {
        return deviceSecure;
    }

    public void setDeviceSecure(Boolean deviceSecure) {
        this.deviceSecure = deviceSecure;
        setField(AppInstanceAtts.deviceSecure, deviceSecure);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
        setField(AppInstanceAtts.language, language);
    }

    public String getDeviceTimezoneOffset() {
        return deviceTimezoneOffset;
    }

    public void setDeviceTimezoneOffset(String deviceTimezoneOffset) {
        this.deviceTimezoneOffset = deviceTimezoneOffset;
        setField(AppInstanceAtts.deviceTimezoneOffset, deviceTimezoneOffset);
    }

    public String getApplicationUserId() {
        return applicationUserId;
    }

    public void setApplicationUserId(String applicationUserId) {
        this.applicationUserId = applicationUserId;
        setField(AppInstanceAtts.applicationUserId, applicationUserId);
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        setField(AppInstanceAtts.deviceName, deviceName);
    }

    public PushServiceType getPushServiceType() {
        return pushServiceType;
    }

    protected void setPushServiceType(PushServiceType pushServiceType) {
        this.pushServiceType = pushServiceType;
        setField(AppInstanceAtts.pushServiceType, pushServiceType.name());
    }

    public String getPushServiceToken() {
        return pushServiceToken;
    }

    protected void setPushServiceToken(String pushServiceToken) {
        this.pushServiceToken = pushServiceToken;
        setField(AppInstanceAtts.pushServiceToken, pushServiceToken);
    }
}
