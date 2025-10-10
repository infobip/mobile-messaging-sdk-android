package org.infobip.mobile.messaging.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.lang.reflect.Field;
import java.util.Locale;

import androidx.annotation.ChecksSdkIntAtLeast;

/**
 * Created by sslavin on 21/04/16.
 */
public class SystemInformation {
    private SystemInformation() {
    }

    private static String systemName = null;
    private static String androidABI = null;
    private static String deviceName = null;

    public static String getAndroidSystemName() {
        if (systemName != null) {
            return systemName;
        }

        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;
            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException | NullPointerException | IllegalAccessException e) {
                MobileMessagingLogger.e("Could not obtain Android system name", e);
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                systemName = fieldName;
            }
        }
        return systemName;
    }

    public static String getAndroidSystemABI() {
        if (androidABI != null) {
            return androidABI;
        }

        androidABI = Build.SUPPORTED_ABIS[0];
        return androidABI;
    }

    public static String getAndroidSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getAndroidSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    @SuppressLint("InlinedApi")
    public static String getAndroidDeviceName(Context context) {
        if (deviceName != null) {
            return deviceName;
        }

        String deviceNameKey = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) ?
                Settings.Global.DEVICE_NAME :
                "device_name";
        deviceName = Settings.Global.getString(context.getContentResolver(), deviceNameKey);

        return deviceName;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    public static boolean isTiramisuOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static boolean isUpsideDownCakeOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
}
