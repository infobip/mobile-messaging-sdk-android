package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;

/**
 * @author mstipanov
 * @since 08.04.2016.
 */
public class PlayServicesSupport {

    public static final int DEVICE_NOT_SUPPORTED = -1;

    private static Boolean isPlayServicesAvailable;
    private final Handler handler = new Handler();

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public void checkPlayServices(final Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(context);

        if (errorCode != ConnectionResult.SUCCESS) {
            isPlayServicesAvailable = false;

            if (apiAvailability.isUserResolvableError(errorCode)) {
                Log.e(MobileMessaging.TAG, "Error accessing GCM.");

            } else {
                errorCode = DEVICE_NOT_SUPPORTED;
                Log.e(MobileMessaging.TAG, "This device is not supported.");
            }

            // Broadcast is not triggered unless it's posted to Main thread queue. See http://stackoverflow.com/a/23917619/2895571
            final int finalErrorCode = errorCode;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent playServicesError = new Intent(Event.GOOGLE_PLAY_SERVICES_ERROR.getKey());
                    playServicesError.putExtra(BroadcastParameter.EXTRA_PLAY_SERVICES_ERROR_CODE, finalErrorCode);

                    context.sendBroadcast(playServicesError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(playServicesError);
                    handler.removeCallbacksAndMessages(null);
                }
            });
            return;
        }

        isPlayServicesAvailable = true;

        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(context, MobileMessagingGcmIntentService.class);
        context.startService(intent);
    }

    public static boolean isPlayServicesAvailable(Context context) {
        if (isPlayServicesAvailable == null) {
            new PlayServicesSupport().checkPlayServices(context);
        }
        return isPlayServicesAvailable;
    }
}
