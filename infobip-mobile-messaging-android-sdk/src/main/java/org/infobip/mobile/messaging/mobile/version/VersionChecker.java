package org.infobip.mobile.messaging.mobile.version;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.Version;

import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 04/10/2016.
 */

public class VersionChecker {

    private static final String TAG = "VersionChecker";

    public void check(final Context context) {

        if (!SoftwareInformation.isDebuggableApplicationBuild(context)) {
            return;
        }

        Long lastCheckTime = PreferenceHelper.findLong(context, MobileMessagingProperty.VERSION_CHECK_LAST_TIME);
        Integer minimumInterval = PreferenceHelper.findInt(context, MobileMessagingProperty.VERSION_CHECK_INTERVAL_DAYS);
        if (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastCheckTime) < minimumInterval) {
            return;
        }

        new VersionCheckTask(context) {
            @Override
            protected void onPostExecute(VersionCheckResult versionCheckResult) {
                super.onPostExecute(versionCheckResult);

                String current = SoftwareInformation.getLibraryVersion();
                if (shouldUpdate(versionCheckResult.getVersion(), current)) {
                    MobileMessagingLogger.w(TAG, "Your library version is outdated, find latest release " + versionCheckResult.getVersion() +
                            " here: " + versionCheckResult.getUpdateUrl());
                }

                PreferenceHelper.saveLong(context, MobileMessagingProperty.VERSION_CHECK_LAST_TIME, System.currentTimeMillis());
            }
        }.execute();
    }

    private boolean shouldUpdate(String latest, String current) {
        Version latestVersion = new Version(latest);
        Version currentVersion = new Version(current);

        return latestVersion.compareTo(currentVersion) > 0;
    }
}
