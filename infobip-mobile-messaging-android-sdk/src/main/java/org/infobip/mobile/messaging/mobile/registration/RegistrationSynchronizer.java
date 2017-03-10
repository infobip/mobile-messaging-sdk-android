package org.infobip.mobile.messaging.mobile.registration;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public class RegistrationSynchronizer extends RetryableSynchronizer {

    private final String registrationId;
    private String deviceApplicationInstanceId;
    private boolean registrationIdSaved;

    public RegistrationSynchronizer(Context context, MobileMessagingStats stats, Executor executor, MobileMessagingCore mobileMessagingCore) {
        super(context, stats, executor);
        this.registrationId = mobileMessagingCore.getRegistrationId();
        this.deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        this.registrationIdSaved = isRegistrationIdReported(context);
    }

    @Override
    public void updatePushRegistrationStatus(final Boolean enabled) {
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(UpsertRegistrationResult result) {
                if (result.hasError() || StringUtils.isBlank(result.getDeviceInstanceId())) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (push registration status update)!");
                    stats.reportError(MobileMessagingStatsError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(result.getError()));
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
                MobileMessagingLogger.e("Error updating registration!");
                setRegistrationIdReported(context, false);

                setPushRegistrationEnabled(context, !MobileMessagingCore.getInstance(context).isPushRegistrationEnabled());
                stats.reportError(MobileMessagingStatsError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);
            }
        }.executeOnExecutor(executor, enabled);
    }

    @Override
    public Task getTask() {
        return Task.UPDATE_REGISTRATION_STATUS;
    }

    @Override
    public void synchronize() {
        if (null != deviceApplicationInstanceId && registrationIdSaved) {
            return;
        }
        reportRegistration(registrationId);
    }

    private void reportRegistration(final String registrationId) {
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(UpsertRegistrationResult result) {
                if (result.hasError() || StringUtils.isBlank(result.getDeviceInstanceId())) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (registration)!");
                    stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
                    retry(result);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(result.getError()));
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                setPushRegistrationEnabled(context, result.getPushRegistrationEnabled());
                setDeviceApplicationInstanceId(context, result.getDeviceInstanceId());
                setRegistrationIdReported(context, true);

                MobileMessagingCore.getInstance(context).reportSystemData();

                Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                registrationCreated.putExtra(BroadcastParameter.EXTRA_GCM_TOKEN, registrationId);
                registrationCreated.putExtra(BroadcastParameter.EXTRA_INFOBIP_ID, result.getDeviceInstanceId());
                context.sendBroadcast(registrationCreated);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
            }

            @Override
            protected void onCancelled(UpsertRegistrationResult result) {
                MobileMessagingLogger.e("Error creating registration!");
                setRegistrationIdReported(context, false);
                retry(result);

                setPushRegistrationEnabled(context, !MobileMessagingCore.getInstance(context).isPushRegistrationEnabled());
                stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
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
