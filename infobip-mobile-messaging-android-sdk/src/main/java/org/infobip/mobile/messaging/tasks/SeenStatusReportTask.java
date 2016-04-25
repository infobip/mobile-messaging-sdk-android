package org.infobip.mobile.messaging.tasks;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SeenStatusReportTask extends AsyncTask<Object, Void, SeenStatusReportResult> {
    private final Context context;

    public SeenStatusReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected SeenStatusReportResult doInBackground(Object... notUsed) {
        MobileMessaging mobileMessaging = MobileMessaging.getInstance(context);
        try {
            String[] messageIDs = mobileMessaging.getUnreportedSeenMessageIds();
            SeenMessages seenMessages = SeenMessages.withMessageIds(messageIDs);
            MobileApiResourceProvider.INSTANCE.getMobileApiSeenStatusReport(context).report(seenMessages.toJson());
            mobileMessaging.removeUnreportedSeenMessageIds(messageIDs);
            return new SeenStatusReportResult(messageIDs);
        } catch (Exception e) {
            mobileMessaging.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error reporting seen status!", e);
            cancel(true);

            Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            registrationSaveError.putExtra("exception", e);
            context.sendBroadcast(registrationSaveError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);

            return null;
        }
    }
}
