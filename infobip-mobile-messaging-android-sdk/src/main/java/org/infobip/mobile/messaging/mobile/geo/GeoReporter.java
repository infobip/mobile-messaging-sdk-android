package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;

import java.util.ArrayList;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporter {

    public void report(final Context context, final MobileMessagingStats stats) {

        final MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        final ArrayList<GeoReport> reports = mobileMessagingCore.removeUnreportedGeoEvents(context);
        if (reports.isEmpty()) {
            return;
        }

        new GeoReportingTask(context) {
            @Override
            protected void onPostExecute(UnsuccessfulResult unused) {
                Intent geoReportsSent = new Intent(Event.GEOFENCE_EVENTS_REPORTED.getKey());
                geoReportsSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_GEOFENCE_REPORTS, reports);
                context.sendBroadcast(geoReportsSent);
                LocalBroadcastManager.getInstance(context).sendBroadcast(geoReportsSent);
            }

            @Override
            protected void onCancelled(UnsuccessfulResult result) {
                stats.reportError(MobileMessagingError.GEO_REPORTING_ERROR);

                mobileMessagingCore.addUnreportedGeoEvents(reports);

                Intent seenStatusReportError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                seenStatusReportError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, result.getError());
                context.sendBroadcast(seenStatusReportError);
                LocalBroadcastManager.getInstance(context).sendBroadcast(seenStatusReportError);
            }
        }.execute(reports.toArray(new GeoReport[reports.size()]));
    }
}
