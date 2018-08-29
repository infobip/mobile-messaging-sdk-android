package org.infobip.mobile.messaging.mobile.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizer {

    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessageHandler mobileMessageHandler;
    private final MRetryPolicy retryPolicy;
    private final MobileApiMessages mobileApiMessages;

    public MessagesSynchronizer(
            MobileMessagingCore mobileMessagingCore,
            MobileMessagingStats stats,
            Executor executor,
            Broadcaster broadcaster,
            MRetryPolicy retryPolicy,
            MobileMessageHandler mobileMessageHandler,
            MobileApiMessages mobileApiMessages) {

        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.retryPolicy = retryPolicy;
        this.mobileApiMessages = mobileApiMessages;
        this.mobileMessageHandler = mobileMessageHandler;
    }

    public void sync() {
        if (!mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will sync messages later");
            return;
        }

        final String[] unreportedMessageIds = mobileMessagingCore.getAndRemoveUnreportedMessageIds();
        new MRetryableTask<Void, List<Message>>() {
            @Override
            public List<Message> run(Void[] objects) {

                String[] messageIds = mobileMessagingCore.getSyncMessagesIds();

                SyncMessagesBody syncMessagesBody = SyncMessagesBody.make(messageIds, unreportedMessageIds);
                MobileMessagingLogger.v("SYNC MESSAGES >>>", syncMessagesBody);
                SyncMessagesResponse syncMessagesResponse = mobileApiMessages.sync(syncMessagesBody);
                MobileMessagingLogger.v("SYNC MESSAGES <<<", syncMessagesResponse);
                return MessagesMapper.mapResponseToMessages(syncMessagesResponse.getPayloads());
            }

            @Override
            public void after(List<Message> messages) {
                broadcaster.deliveryReported(unreportedMessageIds);
                if (messages == null || messages.isEmpty()) {
                    return;
                }

                for (Message message : messages) {
                    mobileMessageHandler.handleMessage(message);
                }
            }

            @Override
            public void error(Throwable error) {
                mobileMessagingCore.addUnreportedMessageIds(unreportedMessageIds);
                mobileMessagingCore.setLastHttpException(error);

                MobileMessagingLogger.e("MobileMessaging API returned error (synchronizing messages)! ", error);
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);

                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
        .retryWith(retryPolicy)
        .execute(executor);
    }
}
