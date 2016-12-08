package org.infobip.mobile.messaging.mobile.registration;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public class RegistrationSynchronizer {

    public void updatePushRegistrationStatus(Context context, String registrationId, MobileMessagingStats stats, Executor executor) {
        updateRegistrationStatus(context, registrationId, stats, executor);
    }

    private void updateRegistrationStatus(final Context context, final String registrationId, final MobileMessagingStats stats, Executor executor) {
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(UpsertRegistrationResult result) {
                if (result.hasError() || StringUtils.isBlank(result.getDeviceInstanceId())) {
                    Log.e(TAG, "MobileMessaging API returned error (push registration status update)!");
                    stats.reportError(MobileMessagingError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                setPushRegistrationEnabled(context, result.getPushRegistrationEnabled());
                setDeviceApplicationInstanceId(context, result.getDeviceInstanceId());
                setRegistrationIdReported(context, true);

                Intent registrationUpdated = new Intent(Event.PUSH_REGISTRATION_ENABLED.getKey());
                registrationUpdated.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, registrationId);
                registrationUpdated.putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, result.getDeviceInstanceId());
                registrationUpdated.putExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, result.getPushRegistrationEnabled());
                context.sendBroadcast(registrationUpdated);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationUpdated);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error creating registration!");
                setRegistrationIdReported(context, false);

                setPushRegistrationEnabled(context, !MobileMessagingCore.getInstance(context).isPushRegistrationEnabled());
                stats.reportError(MobileMessagingError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);
            }
        }.executeOnExecutor(executor);
    }

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
                    Log.e(TAG, "MobileMessaging API returned error (registration)!");
                    stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                setPushRegistrationEnabled(context, result.getPushRegistrationEnabled());
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

                setPushRegistrationEnabled(context, !MobileMessagingCore.getInstance(context).isPushRegistrationEnabled());
                stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);
            }
        }.executeOnExecutor(executor);
    }

    private void setPushRegistrationEnabled(Context context, Boolean pushRegistrationEnabled) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, pushRegistrationEnabled);
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
