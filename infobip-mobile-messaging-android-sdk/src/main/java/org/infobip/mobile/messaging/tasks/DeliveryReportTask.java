package org.infobip.mobile.messaging.tasks;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.registration.DeliveryReportResponse;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeliveryReportTask extends AsyncTask<Object, Void, DeliveryReportResult> {

    @Override
    protected DeliveryReportResult doInBackground(Object... notUsed) {
        try {
            MobileMessaging mobileMessaging = MobileMessaging.getInstance();
            String[] messageIDs = mobileMessaging.getUnreportedMessageIds();
            DeliveryReportResponse report = MobileApiResourceProvider.INSTANCE.getMobileApiDeliveryReport().report(messageIDs);
            mobileMessaging.removeUnreportedMessageIds(messageIDs);
            return new DeliveryReportResult(report, messageIDs);
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, "Error reporting delivery!", e);
            cancel(true);
            return null;
        }
    }
}
