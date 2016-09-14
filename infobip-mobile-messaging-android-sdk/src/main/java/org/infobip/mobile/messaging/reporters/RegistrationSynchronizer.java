package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.UpsertRegistrationResult;
import org.infobip.mobile.messaging.tasks.UpsertRegistrationTask;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public class RegistrationSynchronizer {

    public void synchronize(Context context, String deviceApplicationInstanceId, String registrationId, boolean registrationIdSaved, MobileMessagingStats stats, Executor executor) {
        if (null != deviceApplicationInstanceId && registrationIdSaved) {
            return;
        }

        reportRegistration(context, registrationId, stats, executor);
    }

    private void reportRegistration(final Context context, final String registrationId, final MobileMessagingStats stats, Executor executor) {
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(UpsertRegistrationResult result) {
                if (result.hasError() || StringUtils.isBlank(result.getDeviceInstanceId())) {
                    Log.e(TAG, "MobileMessaging API returned error!");
                    stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                setDeviceApplicationInstanceId(context, result.getDeviceInstanceId());
                setRegistrationIdReported(context, true);

                Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                registrationCreated.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, registrationId);
                registrationCreated.putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, result.getDeviceInstanceId());
                context.sendBroadcast(registrationCreated);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error creating registration!");
                setRegistrationIdReported(context, false);

                stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);
            }
        }.executeOnExecutor(executor);
    }

    private void setDeviceApplicationInstanceId(Context context, String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
    }

    public void setRegistrationIdReported(Context context, boolean registrationIdSaved) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, registrationIdSaved);
    }

    public boolean isRegistrationIdReported(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED);
    }
}
