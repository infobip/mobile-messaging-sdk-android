package org.infobip.mobile.messaging.mobile.data;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import org.infobip.mobile.messaging.MobileMessagingLogger;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class UserDataSynchronizer {

    public void sync(final Context context, final MobileMessagingStats stats, Executor executor, final MobileMessaging.ResultListener<UserData> listener) {

        final UserData userDataToReport = MobileMessagingCore.getInstance(context).getUnreportedUserData();
        if (userDataToReport == null) {
            return;
        }

        new SyncUserDataTask(context) {
            @Override
            protected void onPostExecute(SyncUserDataResult syncUserDataResult) {
                if (syncUserDataResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (user data)!");
                    stats.reportError(MobileMessagingError.USER_DATA_SYNC_ERROR);

                    if (syncUserDataResult.hasInvalidParameterError()) {

                        MobileMessagingCore.getInstance(context).setUserDataReportedWithError();
                        if (listener != null) {
                            listener.onError(syncUserDataResult.getError());
                        }
                    } else {

                        MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");
                        if (listener != null) {
                            listener.onResult(UserData.merge(MobileMessagingCore.getInstance(context).getUserData(), userDataToReport));
                        }
                    }

                    Intent userDataSyncError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    userDataSyncError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, syncUserDataResult.getError());
                    context.sendBroadcast(userDataSyncError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(userDataSyncError);

                    return;
                }

                UserData userData = UserDataMapper.fromUserDataReport(userDataToReport.getExternalUserId(), syncUserDataResult.getPredefined(), syncUserDataResult.getCustom());
                MobileMessagingCore.getInstance(context).setUserDataReported(userData);

                Intent userDataReported = new Intent(Event.USER_DATA_REPORTED.getKey());
                userDataReported.putExtra(BroadcastParameter.EXTRA_USER_DATA, userData.toString());
                context.sendBroadcast(userDataReported);
                LocalBroadcastManager.getInstance(context).sendBroadcast(userDataReported);

                if (listener != null) {
                    listener.onResult(userData);
                }
            }

            @Override
            protected void onCancelled() {
                MobileMessagingLogger.e("Error reporting user data!");
                stats.reportError(MobileMessagingError.USER_DATA_SYNC_ERROR);

                MobileMessagingLogger.v("User data synchronization will be postponed to a later time due to communication error");
                if (listener != null) {
                    listener.onResult(MobileMessagingCore.getInstance(context).getUserData());
                }
            }
        }.executeOnExecutor(executor);
    }
}
