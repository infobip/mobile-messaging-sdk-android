package org.infobip.mobile.messaging.tasks;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.deliveryreports.DeliveryReportResponse;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeliveryReportTask extends AsyncTask<Object, Void, DeliveryReportResult> {
    private final Context context;

    public DeliveryReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected DeliveryReportResult doInBackground(Object... notUsed) {
        try {
            MobileMessaging mobileMessaging = MobileMessaging.getInstance(context);
            String[] messageIDs = mobileMessaging.getUnreportedMessageIds();
            DeliveryReportResponse report = MobileApiResourceProvider.INSTANCE.getMobileApiDeliveryReport(context).report(messageIDs);
            mobileMessaging.removeUnreportedMessageIds(messageIDs);
            return new DeliveryReportResult(report, messageIDs);
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, "Error reporting delivery!", e);
            cancel(true);
            return null;
        }
    }
}
