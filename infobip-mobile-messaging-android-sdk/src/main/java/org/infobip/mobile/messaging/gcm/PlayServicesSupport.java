package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 08.04.2016.
 */
public class PlayServicesSupport {
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public void checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.e(TAG, "Error accessing GCM.");
                //TODO raise event!
            } else {
                Log.i(TAG, "This device is not supported.");
                //TODO raise event!
            }
            return;
        }

        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(context, MobileMessagingGcmIntentService.class);
        context.startService(intent);
    }
}
