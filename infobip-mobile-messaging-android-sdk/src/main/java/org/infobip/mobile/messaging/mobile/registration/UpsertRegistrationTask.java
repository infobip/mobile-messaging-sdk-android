package org.infobip.mobile.messaging.mobile.registration;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.MobileMessagingLogger;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class UpsertRegistrationTask extends AsyncTask<Boolean, Void, UpsertRegistrationResult> {
    private final Context context;

    UpsertRegistrationTask(Context context) {
        this.context = context;
    }

    @Override
    protected UpsertRegistrationResult doInBackground(Boolean... params) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        try {
            String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
            String registrationId = mobileMessagingCore.getRegistrationId();
            Boolean pushRegistrationEnabled = params.length > 0 ? params[0] : null;
            RegistrationResponse registrationResponse = MobileApiResourceProvider.INSTANCE.getMobileApiRegistration(context).upsert(deviceApplicationInstanceId, registrationId, pushRegistrationEnabled);
            return new UpsertRegistrationResult(registrationResponse.getDeviceApplicationInstanceId(), registrationResponse.getPushRegistrationEnabled());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error creating registration!", e);
            cancel(true);

            Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            registrationSaveError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(registrationSaveError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);

            return new UpsertRegistrationResult(e);
        }
    }
}
