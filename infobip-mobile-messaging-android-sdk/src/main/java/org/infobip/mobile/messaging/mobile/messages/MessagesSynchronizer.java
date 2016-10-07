package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;

import java.util.List;
import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

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
                    Log.e(TAG, "MobileMessaging API returned error!");
                    stats.reportError(MobileMessagingError.SYNC_MESSAGES_ERROR);
                    return;
                }

                MobileMessageHandler messageHandler = new MobileMessageHandler();
                List<Message> messages = syncMessagesResult.getMessages();
                if (messages == null) {
                    return;
                }

                for (Message message : messages) {
                    Intent intent = new Intent();
                    intent.putExtras(message.getBundle());
                    messageHandler.handleMessage(context, intent);
                }
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error syncing messages!");
                stats.reportError(MobileMessagingError.SYNC_MESSAGES_ERROR);
            }
        }.executeOnExecutor(executor);
    }
}
