package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemData {

    String sdkVersion;
    String osName;
    String osVersion;
    String deviceManufacturer;
    String deviceModel;
    String applicationName;
    String applicationVersion;
    Boolean geofencing;

    public SystemData(String json) {
        SystemData data = new JsonSerializer().deserialize(json, SystemData.class);
        if (data == null) {
            return;
        }

        this.sdkVersion = data.sdkVersion;
        this.osName = data.osName;
        this.osVersion = data.osVersion;
        this.deviceManufacturer = data.deviceManufacturer;
        this.deviceModel = data.deviceModel;
        this.applicationName = data.applicationName;
        this.applicationVersion = data.applicationVersion;
        this.geofencing = data.geofencing;
    }

    public SystemData(String sdkVersion, String osName, String osVersion, String deviceManufacturer, String deviceModel, String applicationName, String applicationVersion, Boolean geofencing) {
        this.sdkVersion = sdkVersion;
        this.osName = osName;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.geofencing = geofencing;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public String getOsName() {
        return osName;
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

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public Boolean getGeofencing() {
        return geofencing;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = appendToHash(result, prime, sdkVersion);
        result = appendToHash(result, prime, osName);
        result = appendToHash(result, prime, osVersion);
        result = appendToHash(result, prime, deviceManufacturer);
        result = appendToHash(result, prime, deviceModel);
        result = appendToHash(result, prime, applicationName);
        result = appendToHash(result, prime, applicationVersion);
        result = appendToHash(result, prime, geofencing);
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
                StringUtils.isEqual(this.osName, other.osName) &&
                StringUtils.isEqual(this.osVersion, other.osVersion) &&
                StringUtils.isEqual(this.deviceManufacturer, other.deviceManufacturer) &&
                StringUtils.isEqual(this.deviceModel, other.deviceModel) &&
                StringUtils.isEqual(this.applicationName, other.applicationName) &&
                StringUtils.isEqual(this.applicationVersion, other.applicationVersion) &&
                (this.geofencing == other.geofencing);
    }

    @Override
    public String toString() {
        return new JsonSerializer().serialize(this);
    }
}
