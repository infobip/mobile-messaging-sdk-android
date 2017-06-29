package org.infobip.mobile.messaging.mobile.registration;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;

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
            String registrationId = mobileMessagingCore.getRegistrationId();
            Boolean pushRegistrationEnabled = params.length > 0 ? params[0] : null;
            MobileMessagingLogger.v("REGISTRATION >>>", registrationId, pushRegistrationEnabled);
            RegistrationResponse registrationResponse = MobileApiResourceProvider.INSTANCE.getMobileApiRegistration(context).upsert(registrationId, pushRegistrationEnabled);
            MobileMessagingLogger.v("REGISTRATION <<<", registrationResponse);
            return new UpsertRegistrationResult(registrationResponse.getDeviceApplicationInstanceId(), registrationResponse.getPushRegistrationEnabled());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error creating registration!", e);
            cancel(true);
            return new UpsertRegistrationResult(e);
        }
    }
}
