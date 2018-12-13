package org.infobip.mobile.messaging.api.appinstance;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AppInstanceWithPushRegId {

    String pushRegId;
    Boolean regEnabled;
    Boolean notificationsEnabled;
    Boolean geoEnabled;
    String sdkVersion;
    String appVersion;
    String os;
    String osVersion;
    String deviceManufacturer;
    String deviceModel;
    Boolean deviceSecure;
    String osLanguage;
    String deviceTimezoneId;
    String applicationUserId;
    String deviceName;
    Map<String, Object> customAttributes;
    Boolean isPrimary;
    PushServiceType pushServiceType;
    String pushServiceToken;

    public AppInstanceWithPushRegId(String pushRegId) {
        this.pushRegId = pushRegId;
    }
}
