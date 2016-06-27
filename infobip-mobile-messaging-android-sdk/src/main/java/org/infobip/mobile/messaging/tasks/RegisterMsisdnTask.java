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
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.support.ApiException;

import static org.infobip.mobile.messaging.BroadcastParameter.*;

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
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        long msisdn = mobileMessagingCore.getUnreportedMsisdn();
        try {
            MobileApiResourceProvider.INSTANCE.getMobileApiRegisterMsisdn(context).registerMsisdn(mobileMessagingCore.getDeviceApplicationInstanceId(), msisdn);
            return new RegisterMsisdnResult(msisdn);
        } catch (ApiException ae) {
            if ("5".equals(ae.getCode())) {
                onMsisdnValidationError(ae, msisdn);
            } else {
                onApiCommunicationError(ae);
            }
            return null;
        } catch (Exception e) {
            onApiCommunicationError(e);
            return null;
        }
    }

    private void onMsisdnValidationError(Exception e, long msisdn) {
        MobileMessagingCore.getInstance(context).setLastHttpException(e);
        Log.e(MobileMessaging.TAG, "Error syncing MSISDN - did not pass validation!", e);
        cancel(true);

        MobileMessagingCore.getInstance(context).setMsisdnReported(false);

        Intent registrationError = new Intent(Event.API_PARAMETER_VALIDATION_ERROR.getKey());
        registrationError.putExtra(EXTRA_PARAMETER_NAME, EXTRA_MSISDN);
        registrationError.putExtra(EXTRA_PARAMETER_VALUE, msisdn);
        registrationError.putExtra(EXTRA_EXCEPTION, e);
        context.sendBroadcast(registrationError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationError);
    }

    private void onApiCommunicationError(Exception e) {
        MobileMessagingCore.getInstance(context).setLastHttpException(e);
        Log.e(MobileMessaging.TAG, "Error syncing MSISDN!", e);
        cancel(true);

        Intent registrationError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
        registrationError.putExtra(EXTRA_EXCEPTION, e);
        context.sendBroadcast(registrationError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationError);
    }
}
