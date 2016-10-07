package org.infobip.mobile.messaging.util;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * Created by sslavin on 21/04/16.
 */
public class SystemInformation {
    private SystemInformation() {
    }

    private static String systemName = null;
    private static String androidABI = null;

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
            } catch (IllegalArgumentException e) {
                Log.d(TAG, Log.getStackTraceString(e));
            } catch (IllegalAccessException e) {
                Log.d(TAG, Log.getStackTraceString(e));
            } catch (NullPointerException e) {
                Log.d(TAG, Log.getStackTraceString(e));
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            androidABI = Build.CPU_ABI;
        } else {
            androidABI = Build.SUPPORTED_ABIS[0];
        }
        return androidABI;
    }

    public static String getAndroidSystemVersion() {
        return Build.VERSION.RELEASE;
    }
}
