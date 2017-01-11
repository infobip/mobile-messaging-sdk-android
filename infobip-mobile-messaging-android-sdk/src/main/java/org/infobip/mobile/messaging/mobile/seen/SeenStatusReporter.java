package org.infobip.mobile.messaging.mobile.seen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.MobileMessagingLogger;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter {

    private static BatchReporter batchReporter = null;

    public void report(final Context context, String[] unreportedSeenMessageIds, final MobileMessagingStats stats, final Executor executor) {
        if (unreportedSeenMessageIds.length == 0 || !MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            return;
        }

        if (batchReporter == null) {
            batchReporter = new BatchReporter(context);
        }

        batchReporter.put(new Runnable() {
            @Override
            public void run() {
                new SeenStatusReportTask(context) {
                    @Override
                    protected void onPostExecute(SeenStatusReportResult result) {
                        if (result.hasError()) {
                            MobileMessagingLogger.e("MobileMessaging API returned error (seen messages)!");

                            stats.reportError(MobileMessagingError.SEEN_REPORTING_ERROR);
                            Intent seenStatusReportError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                            seenStatusReportError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                            context.sendBroadcast(seenStatusReportError);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(seenStatusReportError);
                            return;
                        }

                        Intent seenReportsSent = new Intent(Event.SEEN_REPORTS_SENT.getKey());
                        Bundle extras = new Bundle();
                        extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, result.getMessageIDs());
                        seenReportsSent.putExtras(extras);
                        context.sendBroadcast(seenReportsSent);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(seenReportsSent);
                    }

                    @Override
                    protected void onCancelled() {
                        stats.reportError(MobileMessagingError.SEEN_REPORTING_ERROR);
                        MobileMessagingLogger.e("Error reporting seen status!");
                    }
                }.executeOnExecutor(executor);
            }
        });
    }
}
