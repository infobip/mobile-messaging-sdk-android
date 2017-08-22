package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 15/07/16.
 */
@SuppressWarnings("unchecked")
public class UserDataReporter {

    private final Context context;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MobileMessagingCore mobileMessagingCore;
    private final MRetryPolicy retryPolicy;
    private final MRetryPolicy noRetryPolicy;
    private final MobileMessagingStats stats;

    public UserDataReporter(Context context, MobileMessagingCore mobileMessagingCore, Executor executor, Broadcaster broadcaster, MRetryPolicy retryPolicy, MobileMessagingStats stats) {
        this.context = context;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.retryPolicy = retryPolicy;
        this.noRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(0)
                .build();
    }

    public void sync(final MobileMessaging.ResultListener listener, final UserData userData) {
        if (userData == null) {
            return;
        }

        if (mobileMessagingCore.shouldSaveUserData()) {
            PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userData.toString());
        }

        new MRetryableTask<UserData, UserData>() {
            @Override
            public UserData run(UserData[] userData) {

                if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
                    MobileMessagingLogger.w("Can't report system data without valid registration");
                    throw InternalSdkError.NO_VALID_REGISTRATION.getException();
                }

                UserDataReport request = UserDataMapper.toUserDataReport(userData[0].getPredefinedUserData(), userData[0].getCustomUserData());
                MobileMessagingLogger.v("USER DATA >>>", request);
                UserDataReport response = MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportUserData(userData[0].getExternalUserId(), request);
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

                if (error instanceof BackendInvalidParameterException) {
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
        .retryWith(retryPolicy(listener != null))
        .execute(executor, userData);
    }

    private MRetryPolicy retryPolicy(boolean hasCallback) {
        return hasCallback ? noRetryPolicy : retryPolicy;
    }
}
