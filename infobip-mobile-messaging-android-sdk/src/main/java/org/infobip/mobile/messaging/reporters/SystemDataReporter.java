package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SystemDataReportResult;
import org.infobip.mobile.messaging.tasks.SystemDataReportTask;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReporter {

    public void report(final Context context, final MobileMessagingStats stats, Executor executor) {

        SystemData data = MobileMessagingCore.getInstance(context).getUnreportedSystemData();
        if (data == null) {
            return;
        }

        SystemDataReport report = new SystemDataReport(data.getSdkVersion(),
                data.getOsVersion(), data.getDeviceManufacturer(), data.getDeviceModel(),
                data.getApplicationVersion(), data.getGeofencing());

        new SystemDataReportTask(context) {
            @Override
            protected void onPostExecute(SystemDataReportResult result) {
                if (result.hasError()) {
                    Log.e(TAG, "MobileMessaging API returned error!");
                    stats.reportError(MobileMessagingError.SYSTEM_DATA_REPORT_ERROR);

                    Intent intent = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    intent.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                    context.sendBroadcast(intent);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    return;
                }

                if (result.isPostponed()) {
                    Log.w(TAG, "System data report is saved and will be sent at a later time");
                    return;
                }

                MobileMessagingCore.getInstance(context).setSystemDataReported();

                Intent dataReported = new Intent(Event.SYSTEM_DATA_REPORTED.getKey());
                dataReported.putExtra(BroadcastParameter.EXTRA_SYSTEM_DATA, result.getData().toString());
                context.sendBroadcast(dataReported);
                LocalBroadcastManager.getInstance(context).sendBroadcast(dataReported);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error reporting user data!");
                stats.reportError(MobileMessagingError.SYSTEM_DATA_REPORT_ERROR);
            }
        }.executeOnExecutor(executor, report);
    }
}
