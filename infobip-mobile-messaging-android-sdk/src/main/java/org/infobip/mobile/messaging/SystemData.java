package org.infobip.mobile.messaging;

import android.os.Bundle;
import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemData {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private final String sdkVersion;
    private final String osVersion;
    private final String deviceManufacturer;
    private final String deviceModel;
    private final String applicationVersion;
    private final boolean geofencing;
    private final boolean notificationsEnabled;
    private final boolean deviceSecure;
    private final String language;
    private final String deviceName;
    private final String deviceTimeZoneOffset;

    public SystemData(String sdkVersion, String osVersion, String deviceManufacturer, String deviceModel, String applicationVersion, boolean geofencing,
                      boolean notificationsEnabled, boolean deviceSecure, String language, String deviceName, String deviceTimeZoneOffset) {
        this.sdkVersion = sdkVersion;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.applicationVersion = applicationVersion;
        this.geofencing = geofencing;
        this.notificationsEnabled = notificationsEnabled;
        this.deviceSecure = deviceSecure;
        this.language = language;
        this.deviceName = deviceName;
        this.deviceTimeZoneOffset = deviceTimeZoneOffset;
    }

    public static SystemData fromJson(String json) {
        return serializer.deserialize(json, SystemData.class);
    }

    public static SystemData createFrom(Bundle bundle) {
        return serializer.deserialize(bundle.getString(BroadcastParameter.EXTRA_SYSTEM_DATA), SystemData.class);
    }

    public String getSdkVersion() {
        return sdkVersion;
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

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public boolean isGeofencing() {
        return geofencing;
    }

    public boolean areNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isDeviceSecure() {
        return deviceSecure;
    }

    public String getLanguage() {
        return language;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceTimeZoneOffset() {
        return deviceTimeZoneOffset;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = appendToHash(result, prime, sdkVersion);
        result = appendToHash(result, prime, osVersion);
        result = appendToHash(result, prime, deviceManufacturer);
        result = appendToHash(result, prime, deviceModel);
        result = appendToHash(result, prime, applicationVersion);
        result = appendToHash(result, prime, geofencing);
        result = appendToHash(result, prime, notificationsEnabled);
        result = appendToHash(result, prime, deviceSecure);
        result = appendToHash(result, prime, language);
        result = appendToHash(result, prime, deviceName);
        result = appendToHash(result, prime, deviceTimeZoneOffset);
        return result;
    }

    private static int appendToHash(int result, int prime, Object o) {
        return prime * result + (o == null ? 0 : o.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        SystemData other = (SystemData) o;
        return StringUtils.isEqual(this.sdkVersion, other.sdkVersion) &&
                StringUtils.isEqual(this.osVersion, other.osVersion) &&
                StringUtils.isEqual(this.deviceManufacturer, other.deviceManufacturer) &&
                StringUtils.isEqual(this.deviceModel, other.deviceModel) &&
                StringUtils.isEqual(this.applicationVersion, other.applicationVersion) &&
                (this.geofencing == other.geofencing) &&
                (this.notificationsEnabled == other.notificationsEnabled) &&
                (this.deviceSecure == other.deviceSecure) &&
                StringUtils.isEqual(this.language, other.language) &&
                StringUtils.isEqual(this.deviceName, other.deviceName) &&
                StringUtils.isEqual(this.deviceTimeZoneOffset, other.deviceTimeZoneOffset);

    }

    @NonNull
    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
