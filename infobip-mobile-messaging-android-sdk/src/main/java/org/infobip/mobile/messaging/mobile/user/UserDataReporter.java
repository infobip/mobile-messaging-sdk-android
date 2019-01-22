package org.infobip.mobile.messaging.mobile.user;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendBaseExceptionWithContent;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.UserMapper.filterOutDeletedData;


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

    public void patch(final MobileMessaging.ResultListener listener, final User user) {
        if (user == null) {
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will patch user data later");
            if (listener != null) {
                listener.onResult(new Result(InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        if (!user.hasDataToReport()) {
            MobileMessagingLogger.w("Attempt to save empty user data, will do nothing");
            if (listener != null) {
                listener.onResult(new Result(InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        new MRetryableTask<User, Void>() {

            @Override
            public Void run(User[] userData) {
                Map<String, Object> request = new HashMap<>(userData[0].getMap());
                MobileMessagingLogger.v("USER DATA >>>", request);
                mobileApiAppInstance.patchUser(mobileMessagingCore.getPushRegistrationId(), false, request);
                MobileMessagingLogger.v("USER DATA <<<");
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
                MobileMessagingLogger.e("MobileMessaging API returned error (user data)! ", error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendBaseExceptionWithContent) {
                    BackendBaseExceptionWithContent errorWithContent = (BackendBaseExceptionWithContent) error;
                    mobileMessagingCore.setUserDataReported(errorWithContent.getContent(User.class), true);

                    if (listener != null) {
                        listener.onResult(new Result(mobileMessagingCore.getUser(), mobileMessagingError));
                    }

                } else if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.setUserDataReportedWithError();

                    if (listener != null) {
                        listener.onResult(new Result(mobileMessagingCore.getUser(), mobileMessagingError));
                    }

                } else {
                    MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");

                    if (listener != null) {
                        listener.onResult(new Result(mobileMessagingCore.getUser()));
                    }
                }

                broadcaster.error(mobileMessagingError);
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor, user);
    }

    public void fetch(final MobileMessaging.ResultListener listener) {

        if (mobileMessagingCore.isRegistrationUnavailable()) {
            MobileMessagingLogger.w("Registration not available yet, will fetch user data later");
            if (listener != null) {
                listener.onResult(new Result(mobileMessagingCore.getUser(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
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
                User user = UserMapper.fromBackend(userResponse);
                mobileMessagingCore.setUserDataReported(user, false);

                saveLatestPrimaryToMyInstallation(user);

                if (listener != null) {
                    listener.onResult(new Result(user));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (user data)! ", error);
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                if (listener != null) {
                    listener.onResult(new Result(mobileMessagingCore.getUser(), MobileMessagingError.createFrom(error)));
                }
            }
        }
                .retryWith(retryPolicy(listener))
                .execute(executor);
    }

    private void saveLatestPrimaryToMyInstallation(User user) {
        if (user.getInstallations() != null) {
            for (Installation installation : user.getInstallations()) {
                if (mobileMessagingCore.getPushRegistrationId() != null &&
                        mobileMessagingCore.getPushRegistrationId().equals(installation.getPushRegistrationId())) {
                    mobileMessagingCore.savePrimarySetting(installation.isPrimaryDevice());
                }
            }
        }
    }

    private MRetryPolicy retryPolicy(MobileMessaging.ResultListener listener) {
        return listener == null && mobileMessagingCore.shouldSaveUserData() ?
                retryPolicyProvider.DEFAULT() : retryPolicyProvider.NO_RETRY();
    }
}
