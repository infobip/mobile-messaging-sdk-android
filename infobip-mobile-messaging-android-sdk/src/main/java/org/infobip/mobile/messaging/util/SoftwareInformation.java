package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.infobip.mobile.messaging.BuildConfig;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;

/**
 * Created by sslavin on 21/04/16.
 */
public class SoftwareInformation {
    private SoftwareInformation() {
    }

    private static String appVersion = null;
    private static String appName = null;
    private static Integer appIconResourceId = null;
    private static String sdkVersionWithPostfixForSystemData = null;
    private static String sdkVersionWithPostfixForUserAgent = null;
    private static NotificationManagerCompat notificationManagerCompat = null;

    public static String getAppVersion(Context context) {
        if (appVersion != null) {
            return appVersion;
        }

        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
        }
        return appVersion;
    }

    public static String getAppName(Context context) {
        if (appName != null) {
            return appName;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            appName = packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (Exception e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
        }
        return appName;
    }

    public static int getAppIconResourceId(Context context) {
        if (appIconResourceId != null) {
            return appIconResourceId;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            appIconResourceId = applicationInfo.icon;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return appIconResourceId != null ? appIconResourceId : 0;
    }

    public static String getSDKVersionWithPostfixForSystemData(Context context) {
        if (sdkVersionWithPostfixForSystemData != null) {
            return sdkVersionWithPostfixForSystemData;
        }

        sdkVersionWithPostfixForSystemData = getSDKVersion();
        String versionPostfix = PreferenceHelper.findString(context, MobileMessagingProperty.SYSTEM_DATA_VERSION_POSTFIX);
        if (versionPostfix != null) {
            sdkVersionWithPostfixForSystemData += " (" + versionPostfix + ")";
        }
        return sdkVersionWithPostfixForSystemData;
    }

    public static String getSDKVersionWithPostfixForUserAgent(Context context) {
        if (sdkVersionWithPostfixForUserAgent != null) {
            return sdkVersionWithPostfixForUserAgent;
        }

        sdkVersionWithPostfixForUserAgent = getSDKVersion();
        String versionPostfix = PreferenceHelper.findString(context, MobileMessagingProperty.SYSTEM_DATA_VERSION_POSTFIX);
        if (versionPostfix != null) {
            sdkVersionWithPostfixForUserAgent += "-" + versionPostfix.replace(" ", "-");
        }
        return sdkVersionWithPostfixForUserAgent;
    }

    public static String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static boolean isDebuggableApplicationBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static boolean areNotificationsEnabled(Context context) {
        if (notificationManagerCompat == null) {
            notificationManagerCompat = NotificationManagerCompat.from(context);
        }

        return notificationManagerCompat.areNotificationsEnabled();
    }
}
