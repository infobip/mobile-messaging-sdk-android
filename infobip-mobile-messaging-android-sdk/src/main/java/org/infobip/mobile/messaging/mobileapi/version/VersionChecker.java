package org.infobip.mobile.messaging.mobileapi.version;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.version.LatestReleaseResponse;
import org.infobip.mobile.messaging.api.version.MobileApiVersion;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
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

public class VersionChecker {

    private static final String TAG = VersionChecker.class.getSimpleName();

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final MobileApiVersion mobileApiVersion;
    private final RetryPolicyProvider retryPolicyProvider;

    public VersionChecker(Context context, MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, MobileApiVersion mobileApiVersion, RetryPolicyProvider retryPolicyProvider) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.mobileApiVersion = mobileApiVersion;
        this.retryPolicyProvider = retryPolicyProvider;
    }

    public void sync() {
        if (!SoftwareInformation.isDebuggableApplicationBuild(context) || usedByPlugin()) {
            return;
        }

        long lastCheckTime = PreferenceHelper.findLong(context, MobileMessagingProperty.VERSION_CHECK_LAST_TIME);
        int minimumInterval = PreferenceHelper.findInt(context, MobileMessagingProperty.VERSION_CHECK_INTERVAL_DAYS);
        if (TimeUnit.MILLISECONDS.toDays(Time.now() - lastCheckTime) < minimumInterval) {
            return;
        }

        new MRetryableTask<Void, VersionCheckResult>() {
            @Override
            public VersionCheckResult run(Void[] voids) {
                MobileMessagingLogger.v("VERSION >>>");
                LatestReleaseResponse response = mobileApiVersion.getLatestRelease();
                MobileMessagingLogger.v("VERSION DONE <<<", response);
                return new VersionCheckResult(response);
            }

            @Override
            public void after(VersionCheckResult versionCheckResult) {
                if (versionCheckResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (version check)!");
                    stats.reportError(MobileMessagingStatsError.VERSION_CHECK_ERROR);
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
            public void error(Throwable error) {
                MobileMessagingLogger.e("Error while checking version!");
                stats.reportError(MobileMessagingStatsError.VERSION_CHECK_ERROR);
            }
        }
        .retryWith(retryPolicyProvider.ONE_RETRY())
        .execute();
    }

    private boolean usedByPlugin() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.SYSTEM_DATA_VERSION_POSTFIX) != null;
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
