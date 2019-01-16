package org.infobip.mobile.messaging.mobile.user;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.UserDataMapper;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
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


@SuppressWarnings("unchecked")
public class UserDataReporter {

    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final MobileApiAppInstance mobileApiAppInstance;
    private final RetryPolicyProvider retryPolicyProvider;

    public UserDataReporter(MobileMessagingCore mobileMessagingCore, Executor executor, Broadcaster broadcaster, RetryPolicyProvider retryPolicyProvider, MobileMessagingStats stats, MobileApiAppInstance mobileApiAppInstance) {
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.mobileApiAppInstance = mobileApiAppInstance;
        this.retryPolicyProvider = retryPolicyProvider;
    }

    public void sync(final MobileMessaging.ResultListener listener, final UserData userData) {
        if (userData == null) {
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will sync user data later");
            return;
        }

        new MRetryableTask<UserData, Void>() {

            @Override
            public Void run(UserData[] userData) {
                UserBody request = UserDataMapper.toUserDataBody(userData[0]);
                if (!UserDataMapper.isUserBodyEmpty(request)) {
                    MobileMessagingLogger.v("USER DATA >>>", request);
                    mobileApiAppInstance.patchUser(mobileMessagingCore.getPushRegistrationId(), false, request);
                    MobileMessagingLogger.v("USER DATA <<<");
                }
                return null;
            }

            @Override
            public void after(Void aVoid) {
                mobileMessagingCore.setUserDataReported(userData, true);

                UserData userDataToReturn = userData;
                if (mobileMessagingCore.shouldSaveUserData()) {
                     userDataToReturn = mobileMessagingCore.getUser();
                }
                broadcaster.userDataReported(userDataToReturn);

                if (listener != null) {
                    listener.onResult(userDataToReturn);
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (user data)! ", error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                if (error instanceof BackendBaseExceptionWithContent) {
                    BackendBaseExceptionWithContent errorWithContent = (BackendBaseExceptionWithContent) error;
                    mobileMessagingCore.setUserDataReported(errorWithContent.getContent(UserData.class), true);

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
                        UserData storedUserData = mobileMessagingCore.getUser();
                        listener.onResult(storedUserData);
                    }
                }

                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor, userData);
    }

    public void fetch(final MobileMessaging.ResultListener listener) {

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will sync user data later");
            return;
        }

        new MRetryableTask<Void, UserBody>() {
            @Override
            public UserBody run(Void[] aVoid) {
                MobileMessagingLogger.v("FETCHING USER DATA >>>");
                UserBody userResponse = mobileApiAppInstance.getUser(mobileMessagingCore.getPushRegistrationId());
                MobileMessagingLogger.v("FETCHING USER DATA <<<", userResponse.toString());
                return userResponse;
            }

            @Override
            public void after(UserBody userResponse) {
                UserData userData = UserDataMapper.createFrom(userResponse);
                mobileMessagingCore.setUserDataReported(userData, false);

                saveLatestPrimaryToMyInstallation(userData);

                if (listener != null) {
                    listener.onResult(userData);
                }

                broadcaster.userDataAcquired(userData);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (user data)! ", error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor);
    }

    private void saveLatestPrimaryToMyInstallation(UserData userData) {
        if (userData.getInstallations() != null) {
            for (UserData.Installation installation : userData.getInstallations()) {
                if (mobileMessagingCore.getPushRegistrationId() != null &&
                        mobileMessagingCore.getPushRegistrationId().equals(installation.getPushRegistrationId())) {
                    mobileMessagingCore.savePrimarySetting(installation.getPrimaryDevice());
                }
            }
        }
    }

    private MRetryPolicy retryPolicy(MobileMessaging.ResultListener listener) {
        return listener == null && mobileMessagingCore.shouldSaveUserData() ?
                retryPolicyProvider.DEFAULT() : retryPolicyProvider.NO_RETRY();
    }
}
