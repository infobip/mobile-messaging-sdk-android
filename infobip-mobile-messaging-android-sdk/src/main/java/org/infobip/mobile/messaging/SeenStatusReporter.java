package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SeenStatusReportResult;
import org.infobip.mobile.messaging.tasks.SeenStatusReportTask;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @auhtor sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter {
    void report(final Context context, RegistrationSynchronizer registrationSynchronizer, String deviceApplicationInstanceId, String registrationId, boolean registrationIdSaved, String[] unreportedSeenMessageIds, final MobileMessagingStats stats) {
        if (unreportedSeenMessageIds.length == 0) {
            return;
        }

        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.w(TAG, "Can't report seen reports to MobileMessaging API without saving registration first!");
            registrationSynchronizer.syncronize(context, deviceApplicationInstanceId, registrationId, registrationIdSaved, stats);
            return;
        }

        new SeenStatusReportTask(context) {
            @Override
            protected void onPostExecute(SeenStatusReportResult result) {
                if (null == result) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");

                    stats.reportError(MobileMessagingError.SEEN_REPORTING_ERROR);
                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    context.sendBroadcast(registrationSaveError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                Intent seenReportsSent = new Intent(Event.SEEN_REPORTS_SENT.getKey());
                Bundle extras = new Bundle();
                extras.putStringArray("messageIDs", result.getMessageIDs());
                seenReportsSent.putExtras(extras);
                context.sendBroadcast(seenReportsSent);
                LocalBroadcastManager.getInstance(context).sendBroadcast(seenReportsSent);
            }

            @Override
            protected void onCancelled() {
                stats.reportError(MobileMessagingError.SEEN_REPORTING_ERROR);
                Log.e(TAG, "Error reporting seen status!");
            }
        }.execute();
    }
}
