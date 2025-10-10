package org.infobip.mobile.messaging.mobileapi.user;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.MobileApiUserData;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendBaseExceptionWithContent;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.UserMapper.filterOutDeletedData;
import static org.infobip.mobile.messaging.util.AuthorizationUtils.getAuthorizationHeader;


@SuppressWarnings("unchecked")
public class UserDataReporter {

    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final MobileApiUserData mobileApiUserData;
    private final RetryPolicyProvider retryPolicyProvider;

    public UserDataReporter(MobileMessagingCore mobileMessagingCore, Executor executor, Broadcaster broadcaster, RetryPolicyProvider retryPolicyProvider, MobileMessagingStats stats, MobileApiUserData mobileApiUserData) {
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.retryPolicyProvider = retryPolicyProvider;
        this.mobileApiUserData = mobileApiUserData;
    }

    public void patch(final MobileMessaging.ResultListener listener, final User user) {
        if (user == null) {
            return;
        }

        final String pushRegistrationId = mobileMessagingCore.getPushRegistrationId();
        if (StringUtils.isBlank(pushRegistrationId)) {
            MobileMessagingLogger.w("Registration not available yet, will patch user data later");
            if (listener != null) {
                listener.onResult(new Result(mobileMessagingCore.getUser(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        if (!user.hasDataToReport()) {
            MobileMessagingLogger.w("Attempt to save empty user data, will do nothing");
            if (listener != null) {
                listener.onResult(new Result(mobileMessagingCore.getUser(), InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        String header = getAuthorizationHeader(mobileMessagingCore, listener);
        if (header == null) {
            return;
        }

        new MRetryableTask<User, Void>() {

            @Override
            public Void run(User[] userData) {
                final Map<String, Object> request = new HashMap<>(userData[0].getMap());
                MobileMessagingLogger.v("USER DATA >>>", request);
                mobileApiUserData.patchUser(pushRegistrationId, header, request);
                MobileMessagingLogger.v("USER DATA DONE <<<");
                return null;
            }

            @Override
            public void after(Void aVoid) {
                mobileMessagingCore.setUserDataReported(user, true);

                User userToReturn = filterOutDeletedData(user);
                if (mobileMessagingCore.shouldSaveUserData()) {
                    userToReturn = mobileMessagingCore.getUser();
                }
                broadcaster.userUpdated(userToReturn);

                if (listener != null) {
                    listener.onResult(new Result(userToReturn));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("USER DATA ERROR <<<", error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (listener != null) {
                    listener.onResult(new Result(mobileMessagingCore.getUser(), mobileMessagingError));
                }

                if (error instanceof BackendBaseExceptionWithContent) {
                    BackendBaseExceptionWithContent errorWithContent = (BackendBaseExceptionWithContent) error;
                    mobileMessagingCore.setUserDataReported(errorWithContent.getContent(User.class), true);
                } else if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                    mobileMessagingCore.setUserDataReportedWithError();
                } else {
                    MobileMessagingLogger.w("User data synchronization will be postponed to a later time due to communication error");
                }

                broadcaster.error(mobileMessagingError);
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor, user);
    }

    public void fetch(final MobileMessaging.ResultListener listener) {

        if (!mobileMessagingCore.isRegistrationAvailable()) {
            MobileMessagingLogger.w("Registration not available yet, you can fetch user data when push registration ID becomes available");
            if (listener != null) {
                listener.onResult(new Result(mobileMessagingCore.getUser(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        String header = getAuthorizationHeader(mobileMessagingCore, listener);
        if (header == null) {
            return;
        }

        new MRetryableTask<Void, UserBody>() {
            @Override
            public UserBody run(Void[] aVoid) {
                MobileMessagingLogger.v("FETCHING USER DATA >>>");
                UserBody userResponse = mobileApiUserData.getUser(mobileMessagingCore.getPushRegistrationId(), header);
                MobileMessagingLogger.v("FETCHING USER DATA DONE<<<", userResponse != null ? userResponse.toString() : null);
                return userResponse;
            }

            @Override
            public void after(UserBody userResponse) {
                User user = UserMapper.fromBackend(userResponse);
                mobileMessagingCore.setUserDataReported(user, false);
                mobileMessagingCore.setShouldRepersonalize(false);

                saveLatestPrimaryToMyInstallation(user);

                if (listener != null) {
                    listener.onResult(new Result(user));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("FETCHING USER DATA ERROR <<<", error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);
                mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);

                if (listener != null) {
                    listener.onResult(new Result(mobileMessagingCore.getUser(), mobileMessagingError));
                }
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor);
    }

    private void saveLatestPrimaryToMyInstallation(User user) {
        if (user != null && user.getInstallations() != null) {
            for (Installation installation : user.getInstallations()) {
                if (mobileMessagingCore.getPushRegistrationId() != null &&
                        mobileMessagingCore.getPushRegistrationId().equals(installation.getPushRegistrationId())) {
                    mobileMessagingCore.savePrimarySetting(installation.isPrimaryDevice());
                    mobileMessagingCore.setPushRegistrationEnabled(installation.isPushRegistrationEnabled());
                }
            }
        }
    }

    private MRetryPolicy retryPolicy(MobileMessaging.ResultListener listener) {
        return listener == null && mobileMessagingCore.shouldSaveUserData() ?
                retryPolicyProvider.DEFAULT() : retryPolicyProvider.NO_RETRY();
    }
}
