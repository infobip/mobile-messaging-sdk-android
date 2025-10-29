/*
 * MobileInboxSynchronizer.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;

import java.util.ArrayList;
import java.util.List;

import static org.infobip.mobile.messaging.util.StringUtils.isBlank;

public class MobileInboxSynchronizer {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final AndroidBroadcaster coreBroadcaster;
    private final MobileInboxBroadcaster mobileInboxBroadcaster;
    private final MobileApiInbox mobileApiInbox;
    private final MRetryPolicy retryPolicy;

    private static final Integer MULTIPLE_TOPICS_FETCH_LIMIT = 1000;

    public MobileInboxSynchronizer(Context context,
                                   MobileMessagingCore mobileMessagingCore,
                                   AndroidBroadcaster coreBroadcaster,
                                   MobileInboxBroadcaster mobileInboxBroadcaster,
                                   MobileApiInbox mobileApiInbox) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.coreBroadcaster = coreBroadcaster;
        this.mobileApiInbox = mobileApiInbox;
        this.mobileInboxBroadcaster = mobileInboxBroadcaster;
        this.retryPolicy = new RetryPolicyProvider(context).DEFAULT();
    }

    public void fetchInbox(String token, String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> listener) {
        if (!mobileMessagingCore.isRegistrationAvailable()) {
            if (listener != null) {
                listener.onResult(new Result<>(InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        new MRetryableTask<Void, FetchInboxResponse>() {

            @Override
            public FetchInboxResponse run(Void[] voids) {
                MobileMessagingLogger.v("FETCHING INBOX >>>");
                String header = token != null ? "Bearer " + token : "App " + mobileMessagingCore.getApplicationCode();
                if (filterOptions == null) {
                    return mobileApiInbox.fetchInbox(externalUserId, header, null, null, null, null);
                }
                String from = filterOptions.getFromDateTime() == null ? null : String.valueOf(filterOptions.getFromDateTime().getTime());
                String to = filterOptions.getToDateTime() == null ? null : String.valueOf(filterOptions.getToDateTime().getTime());
                String topic = isBlank(filterOptions.getTopic()) ? null : filterOptions.getTopic();
                Integer limit = filterOptions.getTopics() == null ? filterOptions.getLimit() : MULTIPLE_TOPICS_FETCH_LIMIT;
                return mobileApiInbox.fetchInbox(externalUserId, header, from, to, topic, limit);
            }

            @Override
            public void after(FetchInboxResponse fetchInboxResponse) {
                if (fetchInboxResponse == null) {
                    MobileMessagingLogger.w("Fetched inbox is empty.");
                    listener.onResult(new Result<>(new Inbox()));
                    return;
                }
                MobileMessagingLogger.v("FETCHING INBOX DONE <<<");
                Inbox inbox = InboxMapper.fromBackend(fetchInboxResponse);
                inbox = filterMessagesByTopics(inbox, filterOptions);
                mobileInboxBroadcaster.inboxFetched(inbox);

                if (listener != null) {
                    listener.onResult(new Result<>(inbox));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("FETCHING INBOX ERROR <<<", error);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                }

                coreBroadcaster.error(mobileMessagingError);
                if (listener != null) {
                    listener.onResult(new Result<>(mobileMessagingError));
                }
            }
        }
                .retryWith(retryPolicy)
                .execute();
    }

    private Inbox filterMessagesByTopics(Inbox inbox, MobileInboxFilterOptions filterOptions) {
        if (filterOptions != null) {
            List<String> topics = filterOptions.getTopics();

            if (topics != null && !topics.isEmpty()) {
                List<InboxMessage> filteredMessages = new ArrayList<>();
                int countUnreadFiltered = 0;
                for (InboxMessage inboxMessage : inbox.getMessages()) {
                    if (topics.contains(inboxMessage.getTopic())) {
                        filteredMessages.add(inboxMessage);
                        if (!inboxMessage.isSeen()) {
                            ++countUnreadFiltered;
                        }
                    }
                }
                inbox.setCountTotalFiltered(filteredMessages.size());
                inbox.setCountUnreadFiltered(countUnreadFiltered);
                filteredMessages = applyLimitToMessages(filteredMessages, filterOptions.getLimit());
                inbox.setMessages(filteredMessages);
            }
        }
        return inbox;
    }

    private List<InboxMessage> applyLimitToMessages(List<InboxMessage> messages, Integer limit) {
        if (limit != null && limit > 0 && messages.size() > limit) {
            return messages.subList(0, limit);
        }
        return messages;
    }
}
