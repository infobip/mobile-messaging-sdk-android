package org.infobip.mobile.messaging.mobile.seen;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter extends RetryableSynchronizer {

    private static BatchReporter batchReporter = null;
    private final Broadcaster broadcaster;

    public SeenStatusReporter(Context context, MobileMessagingStats stats, Executor executor, Broadcaster broadcaster) {
        super(context, stats, executor);
        this.broadcaster = broadcaster;
    }

    public void synchronize() {
        String[] unreportedSeenMessageIds = MobileMessagingCore.getInstance(context).getUnreportedSeenMessageIds();
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
                            MobileMessagingLogger.e("MobileMessaging API returned error (seen messages)!");

                            stats.reportError(MobileMessagingStatsError.SEEN_REPORTING_ERROR);
                            broadcaster.error(MobileMessagingError.createFrom(result.getError()));
                            return;
                        }

                        broadcaster.seenStatusReported(result.getMessageIDs());
                    }

                    @Override
                    protected void onCancelled(SeenStatusReportResult result) {
                        MobileMessagingLogger.e("Error reporting seen status!");
                        stats.reportError(MobileMessagingStatsError.SEEN_REPORTING_ERROR);
                        broadcaster.error(MobileMessagingError.createFrom(result.getError()));
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
