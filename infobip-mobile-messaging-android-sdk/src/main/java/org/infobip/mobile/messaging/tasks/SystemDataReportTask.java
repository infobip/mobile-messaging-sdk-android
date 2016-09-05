package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportTask extends AsyncTask<SystemDataReport, Void, SystemDataReportResult> {

    private final Context context;

    public SystemDataReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected SystemDataReportResult doInBackground(SystemDataReport... params) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.e(MobileMessaging.TAG, "Can't report system data without valid registration!");
            return new SystemDataReportResult(new Exception("Syncing system data: no valid registration"));
        }

        if (params.length < 1) {
            Log.e(MobileMessaging.TAG, "System data is empty, cannot report!");
            return new SystemDataReportResult(new Exception("Syncing system data: request data is empty"));
        }

        SystemDataReport report = params[0];
        try {
            MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportSystemData(deviceApplicationInstanceId, report);
            SystemData data = new SystemData(report.getSdkVersion(),
                    report.getOsVersion(),
                    report.getDeviceManufacturer(),
                    report.getDeviceModel(),
                    report.getApplicationVersion(),
                    report.getGeofencing());
            return new SystemDataReportResult(data);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error reporting system data!", e);
            cancel(true);

            Intent userDataSyncError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            userDataSyncError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(userDataSyncError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(userDataSyncError);
            return new SystemDataReportResult(e);
        }

    }
}
