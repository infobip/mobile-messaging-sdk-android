package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;
import android.os.AsyncTask;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

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
            MobileMessagingLogger.e(InternalSdkError.ERROR_EMPTY_SYSTEM_DATA.get());
            return new SystemDataReportResult(InternalSdkError.ERROR_EMPTY_SYSTEM_DATA.getException());
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
        if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
            MobileMessagingLogger.w("Can't report system data without valid registration");
            return new SystemDataReportResult(data, true);
        }

        try {
            MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportSystemData(report);
            return new SystemDataReportResult(data);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error reporting system data!", e);
            cancel(true);
            return new SystemDataReportResult(e);
        }

    }
}
