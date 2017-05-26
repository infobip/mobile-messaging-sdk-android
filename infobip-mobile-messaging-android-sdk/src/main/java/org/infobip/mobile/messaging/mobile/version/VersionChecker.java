package org.infobip.mobile.messaging.mobile.version;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.Version;

import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 04/10/2016.
 */

public class VersionChecker extends RetryableSynchronizer {

    private static final String TAG = "VersionChecker";

    public VersionChecker(Context context, MobileMessagingStats stats) {
        super(context, stats);
    }

    @Override
    public void synchronize() {
        if (!SoftwareInformation.isDebuggableApplicationBuild(context)) {
            return;
        }

        Long lastCheckTime = PreferenceHelper.findLong(context, MobileMessagingProperty.VERSION_CHECK_LAST_TIME);
        Integer minimumInterval = PreferenceHelper.findInt(context, MobileMessagingProperty.VERSION_CHECK_INTERVAL_DAYS);
        if (TimeUnit.MILLISECONDS.toDays(Time.now() - lastCheckTime) < minimumInterval) {
            return;
        }

        new VersionCheckTask(context) {
            @Override
            protected void onPostExecute(VersionCheckResult versionCheckResult) {
                if (versionCheckResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (version check)!");
                    stats.reportError(MobileMessagingStatsError.VERSION_CHECK_ERROR);
                    retry(versionCheckResult);
                    return;
                }

                String current = SoftwareInformation.getSDKVersion();
                if (shouldUpdate(versionCheckResult.getVersion(), current)) {
                    MobileMessagingLogger.w(TAG, "Your library version is outdated, find latest release " + versionCheckResult.getVersion() +
                            " here: " + versionCheckResult.getUpdateUrl());
                }

                PreferenceHelper.saveLong(context, MobileMessagingProperty.VERSION_CHECK_LAST_TIME, Time.now());
            }

            @Override
            protected void onCancelled(VersionCheckResult versionCheckResult) {
                MobileMessagingLogger.e("Error while checking version!");
                stats.reportError(MobileMessagingStatsError.VERSION_CHECK_ERROR);
                retry(versionCheckResult);
            }
        }.execute();
    }

    @Override
    public Task getTask() {
        return Task.VERSION_CHECK;
    }

    private boolean shouldUpdate(String latest, String current) {
        try {
            Version latestVersion = new Version(latest);
            Version currentVersion = new Version(current);

            return latestVersion.compareTo(currentVersion) > 0;
        } catch (Exception e) {
            MobileMessagingLogger.w(TAG, "Cannot process versions: current(" + current + ") latest(" + latest + ") " + e);
            return false;
        }
    }
}
