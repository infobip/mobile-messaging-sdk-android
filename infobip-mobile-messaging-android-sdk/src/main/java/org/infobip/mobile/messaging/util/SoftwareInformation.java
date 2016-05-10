package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.BuildConfig;

/**
 * Created by sslavin on 21/04/16.
 */
public class SoftwareInformation {
    private SoftwareInformation() {
    }

    private static String appVersion = null;
    private static String appName = null;

    public static String getAppVersion(Context context) {
        if (appVersion != null) {
            return appVersion;
        }

        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return appName;
    }

    public static String getLibraryVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
