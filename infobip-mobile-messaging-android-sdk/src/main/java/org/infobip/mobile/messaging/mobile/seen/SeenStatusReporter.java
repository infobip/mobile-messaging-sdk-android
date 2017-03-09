package org.infobip.mobile.messaging.mobile.seen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter extends RetryableSynchronizer {

    private static BatchReporter batchReporter = null;

    public SeenStatusReporter(Context context, MobileMessagingStats stats, Executor executor) {
        super(context, stats, executor);
    }

    public void synchronize() {
        String[] unreportedMessageIds = MobileMessagingCore.getInstance(context).getUnreportedMessageIds();
        if (unreportedMessageIds.length == 0) {
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

                            stats.reportError(MobileMessagingStatsError.SEEN_REPORTING_ERROR);
                            Intent seenStatusReportError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                            seenStatusReportError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(result.getError()));
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
                    protected void onCancelled(SeenStatusReportResult result) {
                        stats.reportError(MobileMessagingStatsError.SEEN_REPORTING_ERROR);
                        MobileMessagingLogger.e("Error reporting seen status!");
                    }
                }.executeOnExecutor(executor);
            }
        });
    }

    @Override
    public Task getTask() {
        return Task.SEEN_STATUS_REPORT;
    }
}
