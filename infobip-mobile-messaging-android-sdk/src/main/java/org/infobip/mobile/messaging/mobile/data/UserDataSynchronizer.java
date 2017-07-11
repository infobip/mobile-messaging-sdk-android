package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 15/07/16.
 */
@SuppressWarnings("unchecked")
public class UserDataSynchronizer extends RetryableSynchronizer {

    private final Broadcaster broadcaster;
    private final MobileMessagingCore mobileMessagingCore;
    private UserData userDataToReport;

    public UserDataSynchronizer(Context context, MobileMessagingCore mobileMessagingCore, Executor executor, Broadcaster broadcaster) {
        super(context, mobileMessagingCore.getStats(), executor);
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void synchronize(final MobileMessaging.ResultListener listener, final Object object) {
        if (object == null || !(object instanceof UserData)) {
            return;
        }

        userDataToReport = (UserData) object;

        if (mobileMessagingCore.shouldSaveUserData()) {
            PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userDataToReport.toString());
        }

        new SyncUserDataTask(context, userDataToReport) {
            @Override
            protected void onPostExecute(SyncUserDataResult syncUserDataResult) {
                if (syncUserDataResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (user data)!");
                    stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                    if (syncUserDataResult.hasInvalidParameterError()) {

                        mobileMessagingCore.setUserDataReportedWithError();
                        if (listener != null) {
                            listener.onError(MobileMessagingError.createFrom(syncUserDataResult.getError()));
                        }
                    } else {

                        MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");
                        if (listener != null) {
                            performCallbackResult(listener, syncUserDataResult);
                        } else {
                            retry(syncUserDataResult);
                        }
                    }

                    broadcaster.error(MobileMessagingError.createFrom(syncUserDataResult.getError()));
                    return;
                }

                UserData userData = UserDataMapper.fromUserDataReport(userDataToReport.getExternalUserId(), syncUserDataResult.getPredefined(), syncUserDataResult.getCustom());
                mobileMessagingCore.setUserDataReported(userData);

                broadcaster.userDataReported(userData);

                if (listener != null) {
                    listener.onResult(userData);
                }
            }

            @Override
            protected void onCancelled(SyncUserDataResult syncUserDataResult) {
                MobileMessagingLogger.e("Error reporting user data!");
                stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(syncUserDataResult.getError()));

                MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");
                if (listener != null) {
                    performCallbackResult(listener, syncUserDataResult);
                } else {
                    retry(syncUserDataResult);
                }
            }
        }.executeOnExecutor(executor);
    }

    private void performCallbackResult(MobileMessaging.ResultListener listener, SyncUserDataResult result) {
        if (mobileMessagingCore.shouldSaveUserData()) {
            listener.onResult(UserData.merge(mobileMessagingCore.getUserData(), userDataToReport));
        } else {
            listener.onError(MobileMessagingError.createFrom(result.getError()));
        }
    }

    @Override
    public Task getTask() {
        return Task.SYNC_USER_DATA;
    }
}
