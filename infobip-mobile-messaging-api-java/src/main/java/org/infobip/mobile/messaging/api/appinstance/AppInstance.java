package org.infobip.mobile.messaging.api.appinstance;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppInstance {

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
    private String language;
    private String deviceTimezoneOffset;
    private String applicationUserId;
    private String deviceName;
    private Map<String, Object> customAttributes;
    private Boolean isPrimary;
    private PushServiceType pushServiceType;
    private String pushServiceToken;

    public AppInstance(String pushRegId) {
        this.pushRegId = pushRegId;
    }
}
