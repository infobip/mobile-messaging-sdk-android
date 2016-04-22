package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.BuildConfig;

/**
 * Created by sslavin on 21/04/16.
 */
public class SoftwareInformationUtils {
    private SoftwareInformationUtils() {
    }

    private static String appVersion = null;

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

    public static String getLibraryVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
