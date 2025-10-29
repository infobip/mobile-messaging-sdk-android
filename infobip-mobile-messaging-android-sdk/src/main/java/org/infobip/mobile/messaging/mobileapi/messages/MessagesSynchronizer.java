/*
 * MessagesSynchronizer.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.cloud.MobileMessageHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class MessagesSynchronizer {

    private static final long SYNC_MSGS_THROTTLE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessageHandler mobileMessageHandler;
    private final MRetryPolicy retryPolicy;
    private final MobileApiMessages mobileApiMessages;
    private volatile Long lastSyncTimeMillis;

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
        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will patch messages later");
            return;
        }

        final String[] unreportedMessageIds = mobileMessagingCore.getAndRemoveUnreportedMessageIds();
        if (unreportedMessageIds.length == 0 && lastSyncTimeMillis != null &&
                Time.now() - lastSyncTimeMillis < SYNC_MSGS_THROTTLE_INTERVAL_MILLIS ||
                !mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }
        lastSyncTimeMillis = Time.now();

        new MRetryableTask<Void, List<Message>>() {
            @Override
            public List<Message> run(Void[] objects) {
                String[] messageIds = mobileMessagingCore.getSyncMessagesIds();

                SyncMessagesBody syncMessagesBody = SyncMessagesBody.make(messageIds, unreportedMessageIds);
                MobileMessagingLogger.v("SYNC MESSAGES >>>", syncMessagesBody);
                SyncMessagesResponse syncMessagesResponse = mobileApiMessages.sync(syncMessagesBody);
                MobileMessagingLogger.v("SYNC MESSAGES DONE <<<", syncMessagesResponse);
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

                MobileMessagingLogger.e("SYNC MESSAGES ERROR <<<", error);
                stats.reportError(MobileMessagingStatsError.SYNC_MESSAGES_ERROR);

                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(retryPolicy)
                .execute(executor);
    }
}
