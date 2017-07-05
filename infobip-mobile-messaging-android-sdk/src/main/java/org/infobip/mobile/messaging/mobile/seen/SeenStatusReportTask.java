package org.infobip.mobile.messaging.mobile.seen;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

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
            MobileMessagingLogger.e(InternalSdkError.NO_VALID_REGISTRATION.get());
            return new SeenStatusReportResult(InternalSdkError.NO_VALID_REGISTRATION.getException());
        }

        try {
            String messageIDs[] = mobileMessagingCore.getUnreportedSeenMessageIds();
            if (messageIDs.length == 0) {
                return new SeenStatusReportResult(messageIDs);
            }

            SeenMessages seenMessages = SeenMessagesReport.fromMessageIds(messageIDs);
            MobileMessagingLogger.v("SEEN >>>", seenMessages);
            MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).reportSeen(seenMessages);
            MobileMessagingLogger.v("SEEN <<<");
            mobileMessagingCore.removeUnreportedSeenMessageIds(messageIDs);
            return new SeenStatusReportResult(messageIDs);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error reporting seen status!", e);
            cancel(true);

            return new SeenStatusReportResult(e);
        }
    }
}
