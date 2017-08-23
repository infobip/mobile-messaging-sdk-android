package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.notification.NotificationHandler;
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

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessageHandler mobileMessageHandler;
    private final MRetryPolicy retryPolicy;

    public MessagesSynchronizer(Context context, MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, Executor executor, Broadcaster broadcaster, MRetryPolicy retryPolicy, NotificationHandler notificationHandler) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.mobileMessageHandler = new MobileMessageHandler(broadcaster, notificationHandler);
        this.retryPolicy = retryPolicy;
    }

    public void sync() {
        sync(new String[0]);
    }

    public void sync(final String unreportedMessageIds[]) {
        new MRetryableTask<Void, List<Message>>() {
            @Override
            public List<Message> run(Void[] objects) {

                if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
                    MobileMessagingLogger.w("Can't sync messages without valid registration");
                    throw InternalSdkError.NO_VALID_REGISTRATION.getException();
                }

                String[] messageIds = mobileMessagingCore.getSyncMessagesIds();
                String[] unreportedMessageIds = mobileMessagingCore.getAndRemoveUnreportedMessageIds();

                SyncMessagesBody syncMessagesBody = SyncMessagesBody.make(messageIds, unreportedMessageIds);
                MobileMessagingLogger.v("SYNC MESSAGES >>>", syncMessagesBody);
                SyncMessagesResponse syncMessagesResponse = MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).sync(syncMessagesBody);
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
                    mobileMessageHandler.handleMessage(context, message);
                }
            }

            @Override
            public void error(Throwable error) {
                mobileMessagingCore.addUnreportedMessageIds(unreportedMessageIds);
                mobileMessagingCore.setLastHttpException(error);

                MobileMessagingLogger.e("MobileMessaging API returned error (synchronizing messages)! ", error);
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);

                if (!(error instanceof InternalSdkError.InternalSdkException)) {
                    broadcaster.error(MobileMessagingError.createFrom(error));
                }
            }
        }
        .retryWith(retryPolicy)
        .execute(executor);
    }
}
