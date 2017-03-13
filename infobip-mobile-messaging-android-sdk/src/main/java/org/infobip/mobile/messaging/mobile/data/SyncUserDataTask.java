package org.infobip.mobile.messaging.mobile.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

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
            MobileMessagingLogger.e(InternalSdkError.NO_VALID_REGISTRATION.get());
            return new SyncUserDataResult(InternalSdkError.NO_VALID_REGISTRATION.getException());
        }

        UserData userData = mobileMessagingCore.getUnreportedUserData();
        if (userData == null) {
            userData = new UserData();
        }

        try {
            UserDataReport request = UserDataMapper.toUserDataReport(userData.getPredefinedUserData(), userData.getCustomUserData());
            UserDataReport response = MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportUserData(deviceApplicationInstanceId, userData.getExternalUserId(), request);
            return new SyncUserDataResult(response.getPredefinedUserData(), response.getCustomUserData());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error synchronizing user data!", e);
            cancel(true);
            return new SyncUserDataResult(e);
        }
    }
}
