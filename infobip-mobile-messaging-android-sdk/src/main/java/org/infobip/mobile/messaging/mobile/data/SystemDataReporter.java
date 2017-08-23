package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReporter {

    private Context context;
    private MobileMessagingCore mobileMessagingCore;
    private Broadcaster broadcaster;
    private MobileMessagingStats stats;
    private Executor executor;
    private MRetryPolicy policy;

    public SystemDataReporter(Context context, MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, MRetryPolicy policy, Executor executor, Broadcaster broadcaster) {
        this.context = context;
        this.stats = stats;
        this.executor = executor;
        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.policy = policy;
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

                if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
                    MobileMessagingLogger.w("Can't report system data without valid registration");
                    throw InternalSdkError.NO_VALID_REGISTRATION.getException();
                }

                SystemData data = systemDatas[0];
                SystemDataReport report = from(data);
                MobileMessagingLogger.v("SYSTEM DATA >>>", report);
                MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportSystemData(report);
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
                data.isDeviceSecure());
    }
}
