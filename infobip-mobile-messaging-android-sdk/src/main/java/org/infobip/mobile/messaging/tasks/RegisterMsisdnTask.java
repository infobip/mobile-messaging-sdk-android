package org.infobip.mobile.messaging.tasks;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class RegisterMsisdnTask extends AsyncTask<Object, Void, RegisterMsisdnResult> {
    private final Context context;

    public RegisterMsisdnTask(Context context) {
        this.context = context;
    }

    @Override
    protected RegisterMsisdnResult doInBackground(Object... notUsed) {
        MobileMessaging mobileMessaging = MobileMessaging.getInstance(context);
        try {
            long msisdn = mobileMessaging.getMsisdn();
            MobileApiResourceProvider.INSTANCE.getMobileApiRegisterMsisdn(context).registerMsisdn(mobileMessaging.getDeviceApplicationInstanceId(), msisdn);

            return new RegisterMsisdnResult(msisdn);
        } catch (Exception e) {
            mobileMessaging.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error syncing MSISDN!", e);
            cancel(true);

            Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            registrationSaveError.putExtra("exception", e);
            context.sendBroadcast(registrationSaveError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);

            return null;
        }
    }
}
