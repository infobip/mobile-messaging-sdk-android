package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
public class AppInstance extends AppInstanceWithPushRegId {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    transient String pushRegId;

    public AppInstance(String sdkVersion,
                       String osVersion,
                       String deviceManufacturer,
                       String deviceModel,
                       String appVersion,
                       boolean geoEnabled,
                       boolean notificationsEnabled,
                       boolean deviceSecure,
                       String osLanguage,
                       String deviceName,
                       String os) {
        this.sdkVersion = sdkVersion;
        this.osVersion = osVersion;
        this.deviceManufacturer = deviceManufacturer;
        this.deviceModel = deviceModel;
        this.appVersion = appVersion;
        this.geoEnabled = geoEnabled;
        this.notificationsEnabled = notificationsEnabled;
        this.deviceSecure = deviceSecure;
        this.osLanguage = osLanguage;
        this.deviceName = deviceName;
        this.os = os;
    }

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
