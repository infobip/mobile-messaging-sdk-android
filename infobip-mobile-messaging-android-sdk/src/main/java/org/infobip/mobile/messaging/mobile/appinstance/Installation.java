package org.infobip.mobile.messaging.mobile.appinstance;

import org.infobip.mobile.messaging.CustomUserDataValue;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.UserDataMapper;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.PushServiceType;

import java.util.Map;


public class Installation extends UserData.Installation {

    private String applicationUserId;
    private Map<String, CustomUserDataValue> customAttributes;
    private Boolean geoEnabled;
    private String sdkVersion;
    private String appVersion;
    private Boolean deviceSecure;
    private String osLanguage;
    private String deviceTimezoneId;
    private PushServiceType pushServiceType;
    private String pushServiceToken;

    public Installation() {
    }

    protected Installation(Boolean regEnabled, String applicationUserId, Map<String, CustomUserDataValue> customAttributes, Boolean isPrimary, Boolean notificationsEnabled,
                           Boolean geoEnabled, String sdkVersion, String appVersion, String os, String osVersion, String deviceManufacturer, String deviceModel,
                           Boolean deviceSecure, String osLanguage, String deviceTimezoneId, String deviceName, PushServiceType pushServiceType, String pushServiceToken,
                           String pushRegId) {
        this.isPushRegistrationEnabled = regEnabled;
        this.applicationUserId = applicationUserId;
        this.customAttributes = customAttributes;
        this.isPrimaryDevice = isPrimary;
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
        this.pushRegistrationId = pushRegId;
    }

    public void setRegEnabled(Boolean regEnabled) {
        this.isPushRegistrationEnabled = regEnabled;
    }

    public void setApplicationUserId(String applicationUserId) {
        this.applicationUserId = applicationUserId;
    }

    public void setCustomAttributes(Map<String, CustomUserDataValue> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public void setPrimary(Boolean primary) {
        isPrimaryDevice = primary;
    }

    @Override
    public String getPushRegistrationId() {
        return pushRegistrationId;
    }

    public String getApplicationUserId() {
        return applicationUserId;
    }

    public Map<String, CustomUserDataValue> getCustomAttributes() {
        return customAttributes;
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

    public Boolean getDeviceSecure() {
        return deviceSecure;
    }

    public String getOsLanguage() {
        return osLanguage;
    }

    public String getDeviceTimezoneId() {
        return deviceTimezoneId;
    }

    public PushServiceType getPushServiceType() {
        return pushServiceType;
    }

    public String getPushServiceToken() {
        return pushServiceToken;
    }

    public static Installation from(AppInstanceWithPushRegId appInstanceWithPushRegId) {
        return new Installation(appInstanceWithPushRegId.getRegEnabled(),
                appInstanceWithPushRegId.getApplicationUserId(),
                UserDataMapper.mapCustomAttsFromBackendResponse(appInstanceWithPushRegId.getCustomAttributes()),
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
        appInstance.setPushRegId(this.getPushRegistrationId());
        appInstance.setCustomAttributes(UserDataMapper.mapCustomAttsForBackendReport(this.getCustomAttributes()));
        appInstance.setRegEnabled(this.isPushRegistrationEnabled());
        appInstance.setIsPrimary(this.getPrimaryDevice());
        appInstance.setApplicationUserId(this.getApplicationUserId());
        return appInstance;
    }
}
