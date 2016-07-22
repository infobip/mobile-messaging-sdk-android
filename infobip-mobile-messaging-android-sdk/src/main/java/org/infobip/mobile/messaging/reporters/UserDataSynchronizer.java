package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SyncUserDataResult;
import org.infobip.mobile.messaging.tasks.SyncUserDataTask;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class UserDataSynchronizer {

    public void sync(final Context context, final MobileMessagingStats stats, Executor executor) {

        if (MobileMessagingCore.getInstance(context).getUnreportedUserData() == null) {
            return;
        }

        new SyncUserDataTask(context) {
            @Override
            protected void onPostExecute(SyncUserDataResult syncUserDataResult) {
                if (syncUserDataResult.hasError()) {
                    Log.e(TAG, "MobileMessaging API returned error!");
                    stats.reportError(MobileMessagingError.USER_DATA_SYNC_ERROR);

                    if (syncUserDataResult.hasInvalidParameterError()) {
                        MobileMessagingCore.getInstance(context).setUserDataReportedWithError();
                    }

                    Intent userDataSyncError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    userDataSyncError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, syncUserDataResult.getError());
                    context.sendBroadcast(userDataSyncError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(userDataSyncError);
                    return;
                }

                UserData userData = new UserData(syncUserDataResult.getPredefined(), syncUserDataResult.getCustom());
                MobileMessagingCore.getInstance(context).setUserDataReported(userData);

                Intent userDataReported = new Intent(Event.USER_DATA_REPORTED.getKey());
                userDataReported.putExtra(BroadcastParameter.EXTRA_USER_DATA, userData.toString());
                context.sendBroadcast(userDataReported);
                LocalBroadcastManager.getInstance(context).sendBroadcast(userDataReported);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error reporting user data!");
                stats.reportError(MobileMessagingError.USER_DATA_SYNC_ERROR);
            }
        }.executeOnExecutor(executor);
    }
}
