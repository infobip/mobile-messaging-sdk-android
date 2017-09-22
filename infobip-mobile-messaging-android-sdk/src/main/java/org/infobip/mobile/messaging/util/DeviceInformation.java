package org.infobip.mobile.messaging.util;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public static boolean isDeviceSecure(Context context) {
        // Starting with android 6.0 calling isLockScreenDisabled fails altogether because the signature has changed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyguardManager keyguardMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardMgr != null && keyguardMgr.isDeviceSecure();
        }

        try {
            String LOCKSCREEN_UTILS_CLASSNAME = "com.android.internal.widget.LockPatternUtils";
            Class<?> lockUtilsClass = Class.forName(LOCKSCREEN_UTILS_CLASSNAME);
            Object lockUtils = lockUtilsClass.getConstructor(Context.class).newInstance(context);
            Method method = lockUtilsClass.getMethod("getActivePasswordQuality");

            // Starting with android 5.x this fails with InvocationTargetException (caused by SecurityException - MANAGE_USERS permission is
            // required because internally some additional logic was added to return false if one can switch between several users)
            // -> therefore if no exception is thrown, we know the screen lock setting is set to Pattern, PIN/PW or something else other than 'None' or 'Swipe'
            Integer lockProtectionLevel = (Integer) method.invoke(lockUtils);
            return lockProtectionLevel >= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

        } catch (InvocationTargetException ignored) {
        } catch (Exception e) {
            MobileMessagingLogger.e("Error detecting whether screen lock is disabled: " + e);
        }

        return false;
    }
}
