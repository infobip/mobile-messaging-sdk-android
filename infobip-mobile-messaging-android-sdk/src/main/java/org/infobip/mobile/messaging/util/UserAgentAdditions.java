/*
 * UserAgentAdditions.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserAgentAdditions {

    public static String[] getAdditions(Context context) {
        List<String> userAgentAdditions = new ArrayList<>();
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO)) {
            userAgentAdditions.add(SystemInformation.getAndroidSystemName());
            userAgentAdditions.add(SystemInformation.getAndroidSystemVersion());
            userAgentAdditions.add(SystemInformation.getAndroidSystemABI());
            userAgentAdditions.add(DeviceInformation.getDeviceModel());
            userAgentAdditions.add(DeviceInformation.getDeviceManufacturer());
            userAgentAdditions.add(SoftwareInformation.getAppName(context));
            userAgentAdditions.add(SoftwareInformation.getAppVersion(context));
            userAgentAdditions.add(SystemInformation.getAndroidDeviceName(context));
        } else {
            String[] emptySystemInfo = {"", "", "", "", "", "", "", ""};
            userAgentAdditions.addAll(Arrays.asList(emptySystemInfo));
        }
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO)) {
            userAgentAdditions.add(MobileNetworkInformation.getMobileCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileCountryCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCountryCode(context));
        } else {
            String[] emptyCarrierInfoAdditions = {"", "", "", "", "", ""};
            userAgentAdditions.addAll(Arrays.asList(emptyCarrierInfoAdditions));
        }
        List<String> userAgentAdditionsCleaned = new ArrayList<>();
        for (String addition : userAgentAdditions) {
            userAgentAdditionsCleaned.add(removeNotSupportedChars(addition));
        }
        return userAgentAdditionsCleaned.toArray(new String[0]);
    }

    public static String getLibVersion() {
        return System.getProperties().getProperty("library.version");
    }

    /**
     * Related to bug: Caused by: java.lang.IllegalArgumentException: Unexpected char 0x11 at 100 in header value:
     *
     * @param str
     * @return
     */
    public static String removeNotSupportedChars(String str) {
        if (str == null) {
            return null;
        }
        String result = str.replaceAll("[^\\x20-\\x7F]", "");
        if (!str.equals(result)) {
            MobileMessagingLogger.i("SPEC_CHAR", "Removed special char at string: " + str);
        }
        return result;
    }
}
