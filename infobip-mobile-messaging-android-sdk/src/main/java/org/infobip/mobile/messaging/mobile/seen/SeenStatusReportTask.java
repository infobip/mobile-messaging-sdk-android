package org.infobip.mobile.messaging.mobile.seen;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SeenStatusReportTask extends AsyncTask<Object, Void, SeenStatusReportResult> {
    private final Context context;

    SeenStatusReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected SeenStatusReportResult doInBackground(Object... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            MobileMessagingLogger.e("Can't send seen reports to MobileMessaging API without valid registration!");
            return new SeenStatusReportResult(new Exception("No valid registration"));
        }

        try {
            String messageIDs[] = mobileMessagingCore.getUnreportedSeenMessageIds();
            SeenMessages seenMessages = SeenMessagesReport.fromMessageIds(messageIDs);
            MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).reportSeen(seenMessages);
            mobileMessagingCore.removeUnreportedSeenMessageIds(messageIDs);
            return new SeenStatusReportResult(messageIDs);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error reporting seen status!", e);
            cancel(true);

            Intent seenStatusReportError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            seenStatusReportError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(seenStatusReportError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(seenStatusReportError);

            return new SeenStatusReportResult(e);
        }
    }
}
