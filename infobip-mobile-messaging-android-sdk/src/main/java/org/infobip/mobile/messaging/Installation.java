package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceAtts;

import java.util.Map;

import static org.infobip.mobile.messaging.InstallationMapper.fromBundle;

public class Installation extends CustomAttributeHolder {

    private String pushRegId;
    private Boolean regEnabled;
    private Boolean notificationsEnabled;
    private Boolean geoEnabled;
    private String sdkVersion;
    private String appVersion;
    private String os;
    private String osVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private Boolean deviceSecure;
    private String osLanguage;
    private String deviceTimezoneId;
    private String applicationUserId;
    private String deviceName;
    private Boolean isPrimary;
    private PushServiceType pushServiceType;
    private String pushServiceToken;

    public enum PushServiceType {
        GCM,
        Firebase
    }

    public Installation() {

    }

    public Installation(String pushRegId) {
        this.pushRegId = pushRegId;
    }

    public Installation(String pushRegId,
                        Boolean regEnabled,
                        Boolean notificationsEnabled,
                        Boolean geoEnabled,
                        String sdkVersion,
                        String appVersion,
                        String os,
                        String osVersion,
                        String deviceManufacturer,
                        String deviceModel,
                        Boolean deviceSecure,
                        String osLanguage,
                        String deviceTimezoneId,
                        String applicationUserId,
                        String deviceName,
                        Boolean isPrimary,
                        PushServiceType pushServiceType,
                        String pushServiceToken,
                        Map<String, CustomUserDataValue> customAttributes) {
        super(customAttributes);
        this.pushRegId = pushRegId;
        this.regEnabled = regEnabled;
        this.notificationsEnabled = notificationsEnabled;
        this.geoEnabled = geoEnabled;
        this.sdkVersion = sdkVersion;
        this.appVersion = appVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.deviceSecure = deviceSecure;
        this.osLanguage = osLanguage;
        this.deviceTimezoneId = deviceTimezoneId;
        this.applicationUserId = applicationUserId;
        this.deviceName = deviceName;
        this.isPrimary = isPrimary;
        this.pushServiceType = pushServiceType;
        this.pushServiceToken = pushServiceToken;
    }

    public static Installation createFrom(Bundle bundle) {
        return fromBundle(BroadcastParameter.EXTRA_INSTALLATION, bundle);
    }

    public String getPushRegId() {
        return pushRegId;
    }

    void setPushRegId(String pushRegId) {
        this.pushRegId = pushRegId;
        setField(AppInstanceAtts.pushRegId, pushRegId);
    }

    public Boolean getRegEnabled() {
        return regEnabled;
    }

    public void setRegEnabled(Boolean regEnabled) {
        this.regEnabled = regEnabled;
        setField(AppInstanceAtts.regEnabled, regEnabled);
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

    public String getOsLanguage() {
        return osLanguage;
    }

    public void setOsLanguage(String osLanguage) {
        this.osLanguage = osLanguage;
        setField(AppInstanceAtts.osLanguage, osLanguage);
    }

    public String getDeviceTimezoneId() {
        return deviceTimezoneId;
    }

    public void setDeviceTimezoneId(String deviceTimezoneId) {
        this.deviceTimezoneId = deviceTimezoneId;
        setField(AppInstanceAtts.deviceTimezoneId, deviceTimezoneId);
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

    public Boolean getPrimary() {
        return isPrimary;
    }

    public void setPrimary(Boolean primary) {
        isPrimary = primary;
        setField(AppInstanceAtts.isPrimary, primary);
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
