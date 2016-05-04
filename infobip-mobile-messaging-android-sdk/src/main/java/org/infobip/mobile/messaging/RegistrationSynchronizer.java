package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.UpsertRegistrationTask;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
class RegistrationSynchronizer {

    void syncronize(Context context, String deviceApplicationInstanceId, String registrationId, boolean registrationIdSaved, MobileMessagingStats stats) {
        if (null != deviceApplicationInstanceId && registrationIdSaved) {
            return;
        }

        reportRegistration(context, registrationId, stats);
    }

    private void reportRegistration(final Context context, final String registrationId, final MobileMessagingStats stats) {
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(RegistrationResponse registrationResponse) {
                if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");
                    stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }
                setDeviceApplicationInstanceId(context, registrationResponse.getDeviceApplicationInstanceId());
                setRegistrationIdReported(context, true);

                Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                registrationCreated.putExtra("registrationId", registrationId);
                registrationCreated.putExtra("deviceApplicationInstanceId", registrationResponse.getDeviceApplicationInstanceId());
                context.sendBroadcast(registrationCreated);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error creating registration!");
                setRegistrationIdReported(context, false);

                stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);
            }
        }.execute();
    }

    private void setDeviceApplicationInstanceId(Context context, String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
    }

    void setRegistrationIdReported(Context context, boolean registrationIdSaved) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, registrationIdSaved);
    }

    boolean isRegistrationIdReported(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED);
    }
}
