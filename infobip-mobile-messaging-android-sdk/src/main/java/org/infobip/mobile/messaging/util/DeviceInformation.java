package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/**
 * @author sslavin
 * @since 27/04/16.
 */
public class DeviceInformation {
    private DeviceInformation() {
    }

    static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }
}
