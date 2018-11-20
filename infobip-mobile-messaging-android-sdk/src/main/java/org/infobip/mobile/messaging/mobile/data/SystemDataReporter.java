package org.infobip.mobile.messaging.mobile.data;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReporter {

    private final MobileMessagingCore mobileMessagingCore;
    private final Broadcaster broadcaster;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final MRetryPolicy policy;
    private final MobileApiData mobileApiData;

    public SystemDataReporter(MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, MRetryPolicy policy, Executor executor, Broadcaster broadcaster, MobileApiData mobileApiData) {
        this.stats = stats;
        this.executor = executor;
        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.policy = policy;
        this.mobileApiData = mobileApiData;
    }

    public void synchronize() {
        SystemData systemData = mobileMessagingCore.getUnreportedSystemData();
        if (systemData == null) {
            return;
        }

        new MRetryableTask<SystemData, SystemData>() {

            @Override
            public SystemData run(SystemData[] systemDatas) {

                if (systemDatas.length < 1 || systemDatas[0] == null) {
                    MobileMessagingLogger.e(InternalSdkError.ERROR_EMPTY_SYSTEM_DATA.get());
                    throw InternalSdkError.ERROR_EMPTY_SYSTEM_DATA.getException();
                }

                SystemData data = systemDatas[0];
                SystemDataReport report = from(data);
                MobileMessagingLogger.v("SYSTEM DATA >>>", report);
                mobileApiData.reportSystemData(report);
                MobileMessagingLogger.v("SYSTEM DATA <<<");
                return data;
            }

            @Override
            public void after(SystemData systemData) {
                mobileMessagingCore.setSystemDataReported();
                broadcaster.systemDataReported(systemData);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.w("Error reporting system data: " + error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.SYSTEM_DATA_REPORT_ERROR);

                if (!(error instanceof InternalSdkError.InternalSdkException)) {
                    broadcaster.error(MobileMessagingError.createFrom(error));
                }
            }
        }
        .retryWith(policy)
        .execute(executor, systemData);
    }

    @NonNull
    private SystemDataReport from(SystemData data) {
        return new SystemDataReport(
                data.getSdkVersion(),
                data.getOsVersion(),
                data.getDeviceManufacturer(),
                data.getDeviceModel(),
                data.getApplicationVersion(),
                data.isGeofencing(),
                data.areNotificationsEnabled(),
                data.isDeviceSecure(),
                data.getOsLanguage());
    }
}
