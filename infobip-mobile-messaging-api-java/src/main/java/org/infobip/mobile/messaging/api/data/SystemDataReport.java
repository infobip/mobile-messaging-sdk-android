package org.infobip.mobile.messaging.api.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 25/08/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemDataReport {
    String sdkVersion;
    String osVersion;
    String deviceManufacturer;
    String deviceModel;
    String applicationVersion;
    Boolean geofencing;
}
