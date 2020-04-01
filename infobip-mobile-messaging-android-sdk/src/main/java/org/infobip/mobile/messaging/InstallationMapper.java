package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.PushServiceType;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.CustomAttributesMapper.customAttsFromBackend;

public class InstallationMapper {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    public static Installation fromBackend(AppInstance appInstance) {
        return new Installation(
                appInstance.getPushRegId(),
                appInstance.getRegEnabled(),
                appInstance.getNotificationsEnabled(),
                appInstance.getGeoEnabled(),
                appInstance.getSdkVersion(),
                appInstance.getAppVersion(),
                appInstance.getOs(),
                appInstance.getOsVersion(),
                appInstance.getDeviceManufacturer(),
                appInstance.getDeviceModel(),
                appInstance.getDeviceSecure(),
                appInstance.getLanguage(),
                appInstance.getDeviceTimezoneOffset(),
                appInstance.getApplicationUserId(),
                appInstance.getDeviceName(),
                appInstance.getIsPrimary(),
                pushServiceTypeFromBackend(appInstance.getPushServiceType()),
                appInstance.getPushServiceToken(),
                customAttsFromBackend(appInstance.getCustomAttributes()));
    }

    public static AppInstance toBackend(Installation installation) {
        AppInstance appInstance = new AppInstance();
        appInstance.setRegEnabled(installation.isPushRegistrationEnabled());
        appInstance.setNotificationsEnabled(installation.getNotificationsEnabled());
        appInstance.setGeoEnabled(installation.getGeoEnabled());
        appInstance.setSdkVersion(installation.getSdkVersion());
        appInstance.setAppVersion(installation.getAppVersion());
        appInstance.setOs(installation.getOs());
        appInstance.setOsVersion(installation.getOsVersion());
        appInstance.setDeviceManufacturer(installation.getDeviceManufacturer());
        appInstance.setDeviceModel(installation.getDeviceModel());
        appInstance.setDeviceSecure(installation.getDeviceSecure());
        appInstance.setLanguage(installation.getLanguage());
        appInstance.setDeviceTimezoneOffset(installation.getDeviceTimezoneOffset());
        appInstance.setDeviceName(installation.getDeviceName());
        appInstance.setIsPrimary(installation.isPrimaryDevice());
        appInstance.setPushServiceType(pushServiceTypeToBackend(installation.getPushServiceType()));
        appInstance.setPushServiceToken(installation.getPushServiceToken());
        return appInstance;
    }

    public static String toJson(Installation installation) {
        if (installation == null) {
            return null;
        }
        return nullSerializer.serialize(installation);
    }

    public static Installation fromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return nullSerializer.deserialize(json, Installation.class);
    }

    public static Bundle toBundle(String key, Installation installation) {
        Bundle bundle = new Bundle();
        bundle.putString(key, toJson(installation));
        return bundle;
    }

    public static Installation fromBundle(String key, Bundle bundle) {
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }

        return fromJson(bundle.getString(key));
    }

    private static Installation.PushServiceType pushServiceTypeFromBackend(PushServiceType pushServiceType) {
        if (pushServiceType == null) {
            return null;
        }

        switch (pushServiceType) {
            case GCM: return Installation.PushServiceType.GCM;
            case Firebase: return Installation.PushServiceType.Firebase;
            default: return null;
        }
    }

    private static PushServiceType pushServiceTypeToBackend(Installation.PushServiceType pushServiceType) {
        if (pushServiceType == null) {
            return null;
        }

        switch (pushServiceType) {
            case GCM: return PushServiceType.GCM;
            case Firebase: return PushServiceType.Firebase;
            default: return null;
        }
    }
}
