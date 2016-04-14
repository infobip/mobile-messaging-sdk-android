package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.DeliveryReportResult;
import org.infobip.mobile.messaging.tasks.DeliveryReportTask;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
class DeliveryReporter {
    void report(final Context context, RegistrationSynchronizer registrationSynchronizer, String deviceApplicationInstanceId, String registrationId, boolean registrationIdSaved, String[] unreportedMessageIds, final MobileMessagingStats stats) {
        if (unreportedMessageIds.length == 0) {
            return;
        }

        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.w(TAG, "Can't report delivery reports to MobileMessaging API without saving registration first!");
            registrationSynchronizer.syncronize(context, deviceApplicationInstanceId, registrationId, registrationIdSaved, stats);
            return;
        }

        new DeliveryReportTask(context) {
            @Override
            protected void onPostExecute(DeliveryReportResult result) {
                if (null == result) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");

                    stats.reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                Intent messageReceived = new Intent(Event.DELIVERY_REPORTS_SENT.getKey());
                Bundle extras = new Bundle();
                extras.putStringArray("messageIDs", result.getMessageIDs());
                messageReceived.putExtras(extras);
                context.sendBroadcast(messageReceived);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
            }

            @Override
            protected void onCancelled() {
                stats.reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                Log.e(TAG, "Error reporting delivery!");
            }
        }.execute();
    }
}
