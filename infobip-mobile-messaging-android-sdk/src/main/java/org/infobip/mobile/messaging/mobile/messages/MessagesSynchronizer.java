package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author pandric on 09/09/16.
 */
public class MessagesSynchronizer {

    public void synchronize(Context context, MobileMessagingStats stats, Executor executor) {
        syncMessages(context, stats, executor);
    }

    private void syncMessages(final Context context, final MobileMessagingStats stats, Executor executor) {
        new SyncMessagesTask(context) {

            @Override
            protected void onPostExecute(SyncMessagesResult syncMessagesResult) {
                if (syncMessagesResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (synchronizing messages)!");
                    stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
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
            protected void onCancelled() {
                MobileMessagingLogger.e("Error syncing messages!");
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);
            }
        }.executeOnExecutor(executor);
    }
}
