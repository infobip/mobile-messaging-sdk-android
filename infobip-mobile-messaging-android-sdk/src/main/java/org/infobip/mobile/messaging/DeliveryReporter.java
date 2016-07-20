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

import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
class DeliveryReporter {
    void report(final Context context, String[] unreportedMessageIds, final MobileMessagingStats stats, Executor executor) {
        if (unreportedMessageIds.length == 0) {
            return;
        }

        new DeliveryReportTask(context) {
            @Override
            protected void onPostExecute(DeliveryReportResult result) {
                if (result.hasError()) {
                    Log.e(TAG, "MobileMessaging API returned error!");

                    stats.reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    registrationSaveError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                Intent messageReceived = new Intent(Event.DELIVERY_REPORTS_SENT.getKey());
                Bundle extras = new Bundle();
                extras.putStringArray(BroadcastParameter.EXTRA_MESSAGE_IDS, result.getMessageIDs());
                messageReceived.putExtras(extras);
                context.sendBroadcast(messageReceived);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
            }

            @Override
            protected void onCancelled() {
                stats.reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                Log.e(TAG, "Error reporting delivery!");
            }
        }.executeOnExecutor(executor);
    }
}