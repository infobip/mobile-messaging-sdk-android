package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.RegisterMsisdnResult;
import org.infobip.mobile.messaging.tasks.RegisterMsisdnTask;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
class MsisdnSynchronizer {

    void syncronize(Context context, String deviceApplicationInstanceId, String msisdn, boolean msisdnSaved, MobileMessagingStats stats) {
        if (null != deviceApplicationInstanceId && msisdnSaved) {
            return;
        }

        reportRegistration(context, msisdn, stats);
    }

    private void reportRegistration(final Context context, final String msisdn, final MobileMessagingStats stats) {
        if (StringUtils.isBlank(msisdn)) {
            return;
        }

        new RegisterMsisdnTask(context) {
            @Override
            protected void onPostExecute(RegisterMsisdnResult registerMsisdnResult) {
                if (null == registerMsisdnResult) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");
                    stats.reportError(MobileMessagingError.MSISDN_SYNC_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }
                setMsisdnSaved(context, true);

                Intent registrationCreated = new Intent(Event.MSISDN_SYNCED.getKey());
                registrationCreated.putExtra("msisdn", msisdn);
                context.sendBroadcast(registrationCreated);
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error syncing MSISDN registration!");
                setMsisdnSaved(context, false);

                stats.reportError(MobileMessagingError.REGISTRATION_SYNC_ERROR);
            }
        }.execute();
    }

    void setMsisdnSaved(Context context, boolean msisdnSaved) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MSISDN_SAVED, msisdnSaved);
    }

    boolean isMsisdnSaved(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.MSISDN_SAVED);
    }
}
