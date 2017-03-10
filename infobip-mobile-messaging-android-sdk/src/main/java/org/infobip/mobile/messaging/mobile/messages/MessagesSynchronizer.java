package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizer extends RetryableSynchronizer {

    public MessagesSynchronizer(Context context, MobileMessagingStats stats, Executor executor) {
        super(context, stats, executor);
    }

    @Override
    public void synchronize() {
        new SyncMessagesTask(context) {
            @Override
            protected void onPostExecute(SyncMessagesResult syncMessagesResult) {
                if (syncMessagesResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (synchronizing messages)!");
                    stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
                    retry(syncMessagesResult);
                    return;
                }

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
            protected void onCancelled(SyncMessagesResult result) {
                MobileMessagingLogger.e("Error syncing messages!");
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
                retry(result);
            }
        }.executeOnExecutor(executor);
    }

    @Override
    public Task getTask() {
        return Task.SYNC_MESSAGES;
    }
}
