package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemData {

    private String sdkVersion;
    private String osVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private String applicationVersion;
    private boolean geofencing;
    private boolean notificationsEnabled;

    public SystemData(String sdkVersion, String osVersion, String deviceManufacturer, String deviceModel, String applicationVersion, boolean geofencing, boolean notificationsEnabled) {
        this.sdkVersion = sdkVersion;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.applicationVersion = applicationVersion;
        this.geofencing = geofencing;
        this.notificationsEnabled = notificationsEnabled;
    }

    public static SystemData fromJson(String json) {
        return new JsonSerializer().deserialize(json, SystemData.class);
    }

    public static SystemData createFrom(Bundle bundle) {
        return new JsonSerializer().deserialize(bundle.getString(BroadcastParameter.EXTRA_SYSTEM_DATA), SystemData.class);
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

        if (o == null || this.getClass() != o.getClass()){
            return false;
        }

        SystemData other = (SystemData) o;
        return StringUtils.isEqual(this.sdkVersion, other.sdkVersion) &&
                StringUtils.isEqual(this.osVersion, other.osVersion) &&
                StringUtils.isEqual(this.deviceManufacturer, other.deviceManufacturer) &&
                StringUtils.isEqual(this.deviceModel, other.deviceModel) &&
                StringUtils.isEqual(this.applicationVersion, other.applicationVersion) &&
                (this.geofencing == other.geofencing) &&
                (this.notificationsEnabled == other.notificationsEnabled);
    }

    @Override
    public String toString() {
        return new JsonSerializer().serialize(this);
    }
}
