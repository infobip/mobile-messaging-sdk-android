package org.infobip.mobile.messaging.mobile.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
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
    private final UserData userDataToReport;

    SyncUserDataTask(Context context, @NonNull UserData userDataToReport) {
        this.context = context;
        this.userDataToReport = userDataToReport;
    }

    @Override
    protected SyncUserDataResult doInBackground(Void... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
            MobileMessagingLogger.e(InternalSdkError.NO_VALID_REGISTRATION.get());
            return new SyncUserDataResult(InternalSdkError.NO_VALID_REGISTRATION.getException());
        }

        try {
            UserDataReport request = UserDataMapper.toUserDataReport(userDataToReport.getPredefinedUserData(), userDataToReport.getCustomUserData());
            MobileMessagingLogger.v("USER DATA >>>", request);
            UserDataReport response = MobileApiResourceProvider.INSTANCE.getMobileApiData(context).reportUserData(userDataToReport.getExternalUserId(), request);
            MobileMessagingLogger.v("USER DATA <<<", response);
            return new SyncUserDataResult(response.getPredefinedUserData(), response.getCustomUserData());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error synchronizing user data!", e);
            cancel(true);
            return new SyncUserDataResult(e);
        }
    }
}
