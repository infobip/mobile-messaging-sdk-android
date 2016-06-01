package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.infobip.mobile.messaging.Event;

import static org.infobip.mobile.messaging.BroadcastParameter.*;

/**
 * @author sslavin
 * @since 01/06/16.
 */
public class MobileMessagingBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Event.REGISTRATION_ACQUIRED.getKey())) {
            onRegistrationAcquired(context, intent);
        } else if (intent.getAction().equals(Event.REGISTRATION_CREATED.getKey())) {
            onRegistrationCreated(context, intent);
        } else if (intent.getAction().equals(Event.API_COMMUNICATION_ERROR.getKey())) {
            onApiCommunicationError(context, intent);
        } else if (intent.getAction().equals(Event.API_PARAMETER_VALIDATION_ERROR.getKey())) {
            onApiParameterValidationError(context, intent);
        }
    }

    void onRegistrationAcquired(Context context, Intent intent) {
        String gcmToken = intent.getStringExtra(EXTRA_GCM_TOKEN);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(ApplicationPreferences.GCM_TOKEN, gcmToken)
                .apply();
    }

    void onRegistrationCreated(Context context, Intent intent) {
        String gcmToken = intent.getStringExtra(EXTRA_GCM_TOKEN);
        String infobipInternalId = intent.getStringExtra(EXTRA_INFOBIP_ID);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(ApplicationPreferences.GCM_TOKEN, gcmToken)
                .putString(ApplicationPreferences.INFOBIP_REGISTRATION_ID, infobipInternalId)
                .apply();
    }

    void onApiCommunicationError(Context context, Intent intent) {
        Throwable throwable = (Throwable)intent.getSerializableExtra(EXTRA_EXCEPTION);
        String errorDescription = throwable.getMessage() + "\n";
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(ApplicationPreferences.LAST_API_COMMUNICATION_ERROR, errorDescription)
                .apply();
    }

    void onApiParameterValidationError(Context context, Intent intent) {
        Throwable throwable = (Throwable)intent.getSerializableExtra(EXTRA_EXCEPTION);
        String parameterName = intent.getStringExtra(EXTRA_PARAMETER_NAME);
        String errorDescription = throwable.getMessage() + "\n";
        errorDescription += parameterName + "\n";
        if (EXTRA_MSISDN.equals(parameterName)) {
            errorDescription += "" + intent.getLongExtra(EXTRA_PARAMETER_VALUE, 0);
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(ApplicationPreferences.LAST_API_PARAMETER_VALIDATION_ERROR, errorDescription)
                .apply();
    }
}
