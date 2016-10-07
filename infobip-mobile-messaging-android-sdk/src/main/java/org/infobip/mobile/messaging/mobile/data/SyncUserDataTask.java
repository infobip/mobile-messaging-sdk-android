package org.infobip.mobile.messaging.mobile.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author sslavin
 * @since 15/07/16.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SyncUserDataTask extends AsyncTask<Void, Void, SyncUserDataResult> {
    private final Context context;

    SyncUserDataTask(Context context) {
        this.context = context;
    }

    @Override
    protected SyncUserDataResult doInBackground(Void... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.e(MobileMessaging.TAG, "Can't sync user data without valid registration!");
            return new SyncUserDataResult(new Exception("Syncing user data: no valid registration"));
        }

        UserData userData = mobileMessagingCore.getUnreportedUserData();
        if (userData == null) {
            userData = new UserData();
        }

        try {
            UserDataReport request = new UserDataReport(userData.getPredefinedUserData(), userData.getCustomUserData());
            UserDataReport response = MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportUserData(deviceApplicationInstanceId, userData.getExternalUserId(), request);
            return new SyncUserDataResult(response.getPredefinedUserData(), response.getCustomUserData());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error synchronizing user data!", e);
            cancel(true);

            Intent userDataSyncError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            userDataSyncError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(userDataSyncError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(userDataSyncError);
            return new SyncUserDataResult(e);
        }
    }
}
