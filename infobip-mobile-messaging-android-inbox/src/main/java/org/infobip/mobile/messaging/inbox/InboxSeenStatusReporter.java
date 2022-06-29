package org.infobip.mobile.messaging.inbox;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.inbox.InboxSeenMessages;
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
import org.infobip.mobile.messaging.util.StringUtils;

public class InboxSeenStatusReporter {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final AndroidBroadcaster coreBroadcaster;
    private final MobileInboxBroadcaster broadcaster;
    private final MobileApiInbox mobileApiInbox;
    private final MRetryPolicy retryPolicy;

    public InboxSeenStatusReporter(
            Context context,
            MobileMessagingCore mobileMessagingCore,
            AndroidBroadcaster coreBroadcaster,
            MobileInboxBroadcaster broadcaster,
            MobileApiInbox mobileApiInbox
    ) {

        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.coreBroadcaster = coreBroadcaster;
        this.broadcaster = broadcaster;
        this.mobileApiInbox = mobileApiInbox;
        this.retryPolicy = new RetryPolicyProvider(context).DEFAULT();
    }

    public void reportSeen(MobileMessaging.ResultListener<String[]> listener, String externalUserId, String... messageIDs ) {

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("[INBOX] Can't report inbox seen status without valid registration");
            if (listener != null) {
                listener.onResult(new Result<>(messageIDs, InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        new MRetryableTask<Void, String[]>() {
            @Override
            public String[] run(Void[] voids) {
                InboxSeenMessages seenMessages = InboxSeenMessagesMapper.fromMessageIds(externalUserId, messageIDs);
                MobileMessagingLogger.v("[INBOX] SEEN >>>", seenMessages);
                mobileApiInbox.reportSeen(seenMessages);
                MobileMessagingLogger.v("[INBOX] SEEN DONE <<<");
                return messageIDs;
            }

            @Override
            public void after(String[] messageIdsWithTimestamp) {
                broadcaster.seenReported(messageIdsWithTimestamp);
                if (listener != null) {
                    listener.onResult(new Result<>(messageIdsWithTimestamp));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("[INBOX] Error reporting seen status!", error);
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
}
