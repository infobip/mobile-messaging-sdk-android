package org.infobip.mobile.messaging.mobile.data;


import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

public class LogoutUserSynchronizer {
    private final MobileMessagingCore mobileMessagingCore;
    private final Broadcaster broadcaster;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final MRetryPolicy policy;
    private final MobileApiData mobileApiData;

    public LogoutUserSynchronizer(MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, MRetryPolicy policy, Executor executor, Broadcaster broadcaster, MobileApiData mobileApiData) {
        this.stats = stats;
        this.executor = executor;
        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.policy = policy;
        this.mobileApiData = mobileApiData;
    }

    public void sync(final MobileMessaging.ResultListener listener) {
        new MRetryableTask<Void, Void>() {
            @Override
            public Void run(Void[] objects) {

                if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
                    MobileMessagingLogger.w("Can't logout user without valid registration");
                    throw InternalSdkError.NO_VALID_REGISTRATION.getException();
                }

                MobileMessagingLogger.v("LOGOUT USER >>>");
                mobileApiData.logoutUser();
                MobileMessagingLogger.v("LOGOUT USER <<<");
                return null;
            }

            @Override
            public void after(Void objects) {
                mobileMessagingCore.userLoggedOut();
                broadcaster.userLoggedOut();

                if (listener != null) {
                    listener.onResult(objects);
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.w("Error reporting user's logout: " + error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.LOGOUT_USER_REPORT_ERROR);

                if (!(error instanceof InternalSdkError.InternalSdkException)) {
                    broadcaster.error(MobileMessagingError.createFrom(error));
                }

                if (listener != null) {
                    listener.onError(MobileMessagingError.createFrom(error));
                }
            }
        }.retryWith(policy).execute(executor);
    }

}
