package org.infobip.mobile.messaging.inbox;

import static org.infobip.mobile.messaging.util.StringUtils.isBlank;

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

public class MobileInboxSynchronizer {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final AndroidBroadcaster coreBroadcaster;
    private final MobileInboxBroadcaster mobileInboxBroadcaster;
    private final MobileApiInbox mobileApiInbox;
    private final MRetryPolicy retryPolicy;

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
                Integer limit = filterOptions.getLimit();
                return mobileApiInbox.fetchInbox(externalUserId, header, from, to, topic, limit);
            }

            @Override
            public void after(FetchInboxResponse fetchInboxResponse) {
                MobileMessagingLogger.v("FETCHING INBOX DONE <<<");
                Inbox inbox = InboxMapper.fromBackend(fetchInboxResponse);
                mobileInboxBroadcaster.inboxFetched(inbox);

                if (listener != null) {
                    listener.onResult(new Result<>(inbox));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("FETCHING INBOX ERROR <<<", error);
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
