package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SeenStatusReportResult;
import org.infobip.mobile.messaging.tasks.SeenStatusReportTask;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter {

    private static BatchReporter batchReporter = null;

    void report(final Context context, String[] unreportedSeenMessageIds, final MobileMessagingStats stats, final Executor executor) {
        if (unreportedSeenMessageIds.length == 0) {
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
                            Log.e(TAG, "MobileMessaging API returned error!");

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
                        Log.e(TAG, "Error reporting seen status!");
                    }
                }.executeOnExecutor(executor);
            }
        });
    }
}
