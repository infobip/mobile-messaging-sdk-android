package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import org.infobip.mobile.messaging.MobileMessagingLogger;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author sslavin
 * @since 25/08/16.
 */
class SystemDataReportTask extends AsyncTask<SystemDataReport, Void, SystemDataReportResult> {

    private final Context context;

    SystemDataReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected SystemDataReportResult doInBackground(SystemDataReport... params) {

        if (params.length < 1) {
            MobileMessagingLogger.e("System data is empty, cannot report!");
            return new SystemDataReportResult(new Exception("Syncing system data: request data is empty"));
        }

        SystemDataReport report = params[0];
        SystemData data = new SystemData(report.getSdkVersion(),
                report.getOsVersion(),
                report.getDeviceManufacturer(),
                report.getDeviceModel(),
                report.getApplicationVersion(),
                report.getGeofencing(),
                report.getNotificationsEnabled());

        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            MobileMessagingLogger.w("Can't report system data without valid registration");
            return new SystemDataReportResult(data, true);
        }

        try {
            MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportSystemData(deviceApplicationInstanceId, report);
            return new SystemDataReportResult(data);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error reporting system data!", e);
            cancel(true);

            Intent userDataSyncError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            userDataSyncError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(userDataSyncError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(userDataSyncError);
            return new SystemDataReportResult(e);
        }

    }
}
