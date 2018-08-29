package org.infobip.mobile.messaging.mobile.data;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendBaseExceptionWithContent;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 15/07/16.
 */
@SuppressWarnings("unchecked")
public class UserDataReporter {

    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final MobileApiData mobileApiData;
    private final RetryPolicyProvider retryPolicyProvider;

    public UserDataReporter(MobileMessagingCore mobileMessagingCore, Executor executor, Broadcaster broadcaster, RetryPolicyProvider retryPolicyProvider, MobileMessagingStats stats, MobileApiData mobileApiData) {
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.mobileApiData = mobileApiData;
        this.retryPolicyProvider = retryPolicyProvider;
    }

    public void sync(final MobileMessaging.ResultListener listener, final UserData userData) {
        if (userData == null) {
            return;
        }

        mobileMessagingCore.saveUnreportedUserData(userData);

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will sync user data later");
            return;
        }

        new MRetryableTask<UserData, UserData>() {
            @Override
            public UserData run(UserData[] userData) {

                UserDataReport request = UserDataMapper.toUserDataReport(userData[0].getPredefinedUserData(), userData[0].getCustomUserData());
                MobileMessagingLogger.v("USER DATA >>>", request);
                UserDataReport response = mobileApiData.reportUserData(userData[0].getExternalUserId(), request);
                MobileMessagingLogger.v("USER DATA <<<", response);

                return UserDataMapper.fromUserDataReport(userData[0].getExternalUserId(), response.getPredefinedUserData(), response.getCustomUserData());
            }

            @Override
            public void after(UserData userData) {
                mobileMessagingCore.setUserDataReported(userData);
                broadcaster.userDataReported(userData);

                if (listener != null) {
                    listener.onResult(userData);
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (user data)! ", error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                if (error instanceof BackendBaseExceptionWithContent) {
                    BackendBaseExceptionWithContent errorWithContent = (BackendBaseExceptionWithContent) error;
                    mobileMessagingCore.setUserDataReported(errorWithContent.getContent(UserData.class));

                    if (listener != null) {
                        listener.onError(MobileMessagingError.createFrom(error));
                    }

                } else if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.setUserDataReportedWithError();

                    if (listener != null) {
                        listener.onError(MobileMessagingError.createFrom(error));
                    }

                } else {

                    MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");

                    if (listener != null) {
                        listener.onResult(UserData.merge(mobileMessagingCore.getUserData(), userData));
                    }
                }

                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
        .retryWith(retryPolicy(listener))
        .execute(executor, userData);
    }

    private MRetryPolicy retryPolicy(MobileMessaging.ResultListener listener) {
        return listener == null && mobileMessagingCore.shouldSaveUserData() ?
                retryPolicyProvider.DEFAULT() : retryPolicyProvider.NO_RETRY();
    }
}
