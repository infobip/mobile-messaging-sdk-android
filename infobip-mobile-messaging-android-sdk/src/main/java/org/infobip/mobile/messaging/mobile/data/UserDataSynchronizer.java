package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 15/07/16.
 */
@SuppressWarnings("unchecked")
public class UserDataSynchronizer extends RetryableSynchronizer {

    private Broadcaster broadcaster;

    public UserDataSynchronizer(Context context, MobileMessagingStats stats, Executor executor, Broadcaster broadcaster) {
        super(context, stats, executor);
        this.broadcaster = broadcaster;
    }

    @Override
    public void synchronize(final MobileMessaging.ResultListener listener) {
        final UserData userDataToReport = MobileMessagingCore.getInstance(context).getUnreportedUserData();
        if (userDataToReport == null) {
            return;
        }

        new SyncUserDataTask(context) {
            @Override
            protected void onPostExecute(SyncUserDataResult syncUserDataResult) {
                if (syncUserDataResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (user data)!");
                    stats.reportError(MobileMessagingStatsError.USER_DATA_SYNC_ERROR);

                    if (syncUserDataResult.hasInvalidParameterError()) {

                        MobileMessagingCore.getInstance(context).setUserDataReportedWithError();
                        if (listener != null) {
                            listener.onError(MobileMessagingError.createFrom(syncUserDataResult.getError()));
                        }
                    } else {

                        MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");
                        if (listener != null) {
                            listener.onResult(UserData.merge(MobileMessagingCore.getInstance(context).getUserData(), userDataToReport));
                        } else {
                            retry(syncUserDataResult);
                        }
                    }

                    broadcaster.error(MobileMessagingError.createFrom(syncUserDataResult.getError()));
                    return;
                }

                UserData userData = UserDataMapper.fromUserDataReport(userDataToReport.getExternalUserId(), syncUserDataResult.getPredefined(), syncUserDataResult.getCustom());
                MobileMessagingCore.getInstance(context).setUserDataReported(userData);

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
                    listener.onResult(MobileMessagingCore.getInstance(context).getUserData());
                } else {
                    retry(syncUserDataResult);
                }
            }
        }.executeOnExecutor(executor);
    }

    @Override
    public Task getTask() {
        return Task.SYNC_USER_DATA;
    }
}
