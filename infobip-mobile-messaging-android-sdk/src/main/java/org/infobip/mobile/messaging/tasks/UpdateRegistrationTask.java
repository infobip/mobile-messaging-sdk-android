package org.infobip.mobile.messaging.tasks;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class UpdateRegistrationTask extends AsyncTask<Object, Void, RegistrationResponse> {

    @Override
    protected RegistrationResponse doInBackground(Object... notUsed) {
        try {
            return MobileApiResourceProvider.INSTANCE.getMobileApiRegistration().update(MobileMessaging.getInstance().getInfobipRegistrationId(), MobileMessaging.getInstance().getRegistrationId());
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, "Error updating registration!", e);
            cancel(true);
            return null;
        }
    }
}
