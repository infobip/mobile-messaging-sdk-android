package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.infobip.mobile.messaging.BuildConfig;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * Created by sslavin on 21/04/16.
 */
public class SoftwareInformation {
    private SoftwareInformation() {
    }

    private static String appVersion = null;
    private static String appName = null;
    private static Integer appIconResourceId = null;

    public static String getAppVersion(Context context) {
        if (appVersion != null) {
            return appVersion;
        }

        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, Log.getStackTraceString(e));
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
            Log.d(TAG, Log.getStackTraceString(e));
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

    public static String getLibraryVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static boolean isDebuggableApplicationBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
