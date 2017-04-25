package org.infobip.mobile.messaging.mobile.seen;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.SeenBody;
import org.infobip.mobile.messaging.api.messages.reporting.MessageReport;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
            SeenBody seenBody = fromMultipleReports(messageIDs);
            MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).reportSeen(seenBody);
            mobileMessagingCore.removeUnreportedSeenMessageIds(messageIDs);
            return new SeenStatusReportResult(messageIDs);
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error reporting seen status!", e);
            cancel(true);

            return new SeenStatusReportResult(e);
        }
    }

    @NonNull
    private static SeenBody fromMultipleReports(String serializedMessageReports[]) {
        List<MessageReport> reports = new ArrayList<>();
        for (String seenMessage : serializedMessageReports) {
            String[] messageIdWithTimestamp = seenMessage.split(StringUtils.COMMA_WITH_SPACE);
            String messageId = messageIdWithTimestamp[0];
            String seenTimestampString = messageIdWithTimestamp[1];

            long seenTimestamp = Long.valueOf(seenTimestampString);
            long deltaTimestamp = Time.now() - seenTimestamp;
            long deltaInSeconds = Math.round((float) deltaTimestamp / 1000);

            reports.add(new MessageReport(messageId, deltaInSeconds));
        }
        return new SeenBody(reports.toArray(new MessageReport[reports.size()]));
    }
}