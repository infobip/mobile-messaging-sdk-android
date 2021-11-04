package org.infobip.mobile.messaging.cloud;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.apiavailability.ApiAvailability;

/**
 * @author mstipanov
 * @since 08.04.2016.
 */
public class PlayServicesSupport {

    public static final int DEVICE_NOT_SUPPORTED = -1;

    private static Boolean isPlayServicesAvailable;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static ApiAvailability apiAvailability = new ApiAvailability();

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public void checkPlayServicesAndTryToAcquireToken(final Context context, boolean shouldResetToken, @Nullable MobileMessaging.InitListener initListener, FirebaseAppProvider firebaseAppProvider) {
        int errorCode = apiAvailability.checkServicesStatus(context);
        isPlayServicesAvailable = errorCode == ConnectionResult.SUCCESS;
        if (errorCode != ConnectionResult.SUCCESS) {

            InternalSdkError internalSdkError;
            if (apiAvailability.isUserResolvableError(errorCode)) {
                internalSdkError = InternalSdkError.ERROR_ACCESSING_PLAY_SERVICES;
            } else {
                errorCode = DEVICE_NOT_SUPPORTED;
                internalSdkError = InternalSdkError.DEVICE_NOT_SUPPORTED;
            }

            final int finalErrorCode = errorCode;
            if (initListener != null) {
                initListener.onError(internalSdkError, finalErrorCode);
            }
            MobileMessagingLogger.e(internalSdkError.get() + ". google error code: " + errorCode + ", see com.google.android.gms.common.ConnectionResult");

            // Broadcast is not triggered unless it's posted to Main thread queue. See http://stackoverflow.com/a/23917619/2895571
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

        if (shouldResetToken) {
            MobileMessagingCloudService.enqueueTokenReset(context, firebaseAppProvider);
        } else {
            MobileMessagingCloudService.enqueueTokenAcquisition(context, firebaseAppProvider);
        }

        if (initListener != null) {
            initListener.onSuccess();
        }
    }

    public static boolean isPlayServicesAvailable(Context context) {
        if (isPlayServicesAvailable != null) {
            return isPlayServicesAvailable;
        }

        isPlayServicesAvailable = apiAvailability.isServicesAvailable(context);
        return isPlayServicesAvailable;
    }
}
