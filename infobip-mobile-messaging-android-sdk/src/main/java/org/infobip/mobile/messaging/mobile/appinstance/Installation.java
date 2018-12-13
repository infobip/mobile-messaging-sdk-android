package org.infobip.mobile.messaging.mobile.appinstance;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.PushServiceType;

import java.util.Map;


public class Installation {

    private Boolean regEnabled;
    private String applicationUserId;
    private Map<String, Object> customAttributes;
    private Boolean isPrimary;
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
    private String deviceName;
    private PushServiceType pushServiceType;
    private String pushServiceToken;
    private String pushRegId;

    public Installation() {
    }

    protected Installation(Boolean regEnabled, String applicationUserId, Map<String, Object> customAttributes, Boolean isPrimary, Boolean notificationsEnabled,
                           Boolean geoEnabled, String sdkVersion, String appVersion, String os, String osVersion, String deviceManufacturer, String deviceModel,
                           Boolean deviceSecure, String osLanguage, String deviceTimezoneId, String deviceName, PushServiceType pushServiceType, String pushServiceToken,
                           String pushRegId) {
        this.regEnabled = regEnabled;
        this.applicationUserId = applicationUserId;
        this.customAttributes = customAttributes;
        this.isPrimary = isPrimary;
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
        this.deviceName = deviceName;
        this.pushServiceType = pushServiceType;
        this.pushServiceToken = pushServiceToken;
        this.pushRegId = pushRegId;
    }

    public void setRegEnabled(Boolean regEnabled) {
        this.regEnabled = regEnabled;
    }

    public void setApplicationUserId(String applicationUserId) {
        this.applicationUserId = applicationUserId;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public void setPrimary(Boolean primary) {
        isPrimary = primary;
    }

    public Boolean getRegEnabled() {
        return regEnabled;
    }

    public String getApplicationUserId() {
        return applicationUserId;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public Boolean getPrimary() {
        return isPrimary;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public Boolean getGeoEnabled() {
        return geoEnabled;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public Boolean getDeviceSecure() {
        return deviceSecure;
    }

    public String getOsLanguage() {
        return osLanguage;
    }

    public String getDeviceTimezoneId() {
        return deviceTimezoneId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public PushServiceType getPushServiceType() {
        return pushServiceType;
    }

    public String getPushServiceToken() {
        return pushServiceToken;
    }

    public String getPushRegId() {
        return pushRegId;
    }

    public static Installation from(AppInstanceWithPushRegId appInstanceWithPushRegId) {
        return new Installation(appInstanceWithPushRegId.getRegEnabled(),
                appInstanceWithPushRegId.getApplicationUserId(),
                appInstanceWithPushRegId.getCustomAttributes(),
                appInstanceWithPushRegId.getIsPrimary(),
                appInstanceWithPushRegId.getNotificationsEnabled(),
                appInstanceWithPushRegId.getGeoEnabled(),
                appInstanceWithPushRegId.getSdkVersion(),
                appInstanceWithPushRegId.getAppVersion(),
                appInstanceWithPushRegId.getOs(),
                appInstanceWithPushRegId.getOsVersion(),
                appInstanceWithPushRegId.getDeviceManufacturer(),
                appInstanceWithPushRegId.getDeviceModel(),
                appInstanceWithPushRegId.getDeviceSecure(),
                appInstanceWithPushRegId.getOsLanguage(),
                appInstanceWithPushRegId.getDeviceTimezoneId(),
                appInstanceWithPushRegId.getDeviceName(),
                appInstanceWithPushRegId.getPushServiceType(),
                appInstanceWithPushRegId.getPushServiceToken(),
                appInstanceWithPushRegId.getPushRegId());
    }

    public AppInstance toAppInstance() {
        AppInstance appInstance = new AppInstance();
        appInstance.setPushRegId(this.getPushRegId());
        appInstance.setCustomAttributes(this.getCustomAttributes());
        appInstance.setRegEnabled(this.getRegEnabled());
        appInstance.setIsPrimary(this.getPrimary());
        appInstance.setApplicationUserId(this.getApplicationUserId());
        return appInstance;
    }
}
