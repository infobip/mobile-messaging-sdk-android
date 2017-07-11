package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReporter extends RetryableSynchronizer {

    private Broadcaster broadcaster;

    public SystemDataReporter(Context context, MobileMessagingStats stats, Executor executor, Broadcaster broadcaster) {
        super(context, stats, executor);
        this.broadcaster = broadcaster;
    }

    @Override
    public void synchronize() {
        SystemData data = MobileMessagingCore.getInstance(context).getUnreportedSystemData();
        if (data == null) {
            return;
        }

        SystemDataReport report = new SystemDataReport(data.getSdkVersion(),
                data.getOsVersion(), data.getDeviceManufacturer(), data.getDeviceModel(),
                data.getApplicationVersion(), data.isGeofencing(), data.areNotificationsEnabled(), data.isDeviceSecure());

        new SystemDataReportTask(context) {
            @Override
            protected void onPostExecute(SystemDataReportResult result) {
                if (result.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (system data)!");
                    stats.reportError(MobileMessagingStatsError.SYSTEM_DATA_REPORT_ERROR);
                    broadcaster.error(MobileMessagingError.createFrom(result.getError()));
                    retry(result);
                    return;
                }

                if (result.isPostponed()) {
                    MobileMessagingLogger.w("System data report is saved and will be sent at a later time");
                    return;
                }

                MobileMessagingCore.getInstance(context).setSystemDataReported();
                broadcaster.systemDataReported(result.getData());
            }

            @Override
            protected void onCancelled(SystemDataReportResult result) {
                MobileMessagingLogger.e("Error reporting user data!");
                stats.reportError(MobileMessagingStatsError.SYSTEM_DATA_REPORT_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(result.getError()));
                retry(result);
            }
        }.executeOnExecutor(executor, report);
    }

    @Override
    public Task getTask() {
        return Task.SYSTEM_DATA_REPORT;
    }
}
