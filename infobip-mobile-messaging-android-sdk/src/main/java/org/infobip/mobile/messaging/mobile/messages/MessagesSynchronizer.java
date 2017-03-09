package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author pandric on 09/09/16.
 */
public class MessagesSynchronizer {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private Handler handler = new Handler(Looper.getMainLooper());

    public void synchronize(Context context, MobileMessagingStats stats, Executor executor) {
        scheduleSyncMessagesTask(context, stats, executor, 0);
    }

    private void startSyncMessagesTask(final Context context, final MobileMessagingStats stats, final Executor executor, final int numOfAttemptsDone) {
        new SyncMessagesTask(context) {
            @Override
            protected void onPostExecute(SyncMessagesResult syncMessagesResult) {
                if (syncMessagesResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (synchronizing messages)!");
                    stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
                    scheduleSyncMessagesTask(context, stats, executor, numOfAttemptsDone + 1);
                    return;
                }

                handler.removeCallbacksAndMessages(null);
                MobileMessageHandler messageHandler = new MobileMessageHandler();
                List<Message> messages = syncMessagesResult.getMessages();
                if (messages == null) {
                    return;
                }

                for (Message message : messages) {
                    messageHandler.handleMessage(context, message);
                }
            }

            @Override
            protected void onCancelled() {
                MobileMessagingLogger.e("Error syncing messages!");
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
                scheduleSyncMessagesTask(context, stats, executor, numOfAttemptsDone + 1);
            }
        }.executeOnExecutor(executor);
    }

    private void scheduleSyncMessagesTask(final Context context, final MobileMessagingStats stats, final Executor executor, final int numOfAttemptsDone) {
        if (numOfAttemptsDone > DEFAULT_MAX_RETRY_COUNT) {
            MobileMessagingLogger.w("No more retries scheduled for message sync");
            return;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startSyncMessagesTask(context, stats, executor, numOfAttemptsDone);
            }
        }, TimeUnit.SECONDS.toMillis(numOfAttemptsDone * numOfAttemptsDone * 2));
    }
}
