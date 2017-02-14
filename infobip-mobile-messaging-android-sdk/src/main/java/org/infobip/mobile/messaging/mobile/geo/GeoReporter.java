package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.geo.GeoAreasHandler;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.geo.GeoReportHelper;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.Arrays;
import java.util.List;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporter {

    /**
     * Reports geo events to server asynchronously and produces {@link Event#GEOFENCE_AREA_ENTERED}
     */
    public void report(final Context context) {

        final MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        final GeoReport reports[] = mobileMessagingCore.removeUnreportedGeoEvents();
        if (reports.length == 0 || !mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        new GeoReportingTask(context) {
            @Override
            protected void onPostExecute(GeoReportingResult result) {
                GeoReporter.handleSuccess(context, reports, result);
                GeoAreasHandler.handleGeoReportingResult(context, result);
            }

            @Override
            protected void onCancelled(GeoReportingResult result) {
                GeoReporter.handleError(context, result.getError(), reports);
                GeoAreasHandler.handleGeoReportingResult(context, result);
            }
        }.execute(reports);
    }

    /**
     * Reports geo events synchronously
     * @param geoReports set that contains original messages and corresponding events
     * @return result that will contain lists of campaign ids and mappings between library generated message ids and IPCore ids
     */
    public static @NonNull GeoReportingResult reportSync(@NonNull Context context, @NonNull GeoReport geoReports[]) {
        try {
            GeoReportingResult result = GeoReportingTask.executeSync(context, geoReports);
            GeoReporter.handleSuccess(context, geoReports, result);
            return result;
        } catch (Exception e) {
            GeoReporter.handleError(context, e, geoReports);
            return new GeoReportingResult(e);
        }
    }

    /**
     * Handles successful reporting of events to the server, will send out events with what was reported to the server.
     * @param geoReports reports that were sent to the server.
     * @param result result from the server
     */
    private static void handleSuccess(Context context, GeoReport geoReports[], GeoReportingResult result) {
        List<GeoReport> geoReportsToBroadcast = GeoReportHelper.filterOutNonActiveReports(context, Arrays.asList(geoReports), result);
        if (geoReportsToBroadcast.isEmpty()) {
            return;
        }

        Intent geoReportsSent = new Intent(Event.GEOFENCE_EVENTS_REPORTED.getKey());
        geoReportsSent.putExtras(BundleMapper.geoReportsToBundle(geoReportsToBroadcast));
        context.sendBroadcast(geoReportsSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(geoReportsSent);
    }

    /**
     * Handles any error that happens during reporting to the server.
     * @param error error that happens
     * @param geoReports reports sent to server
     */
    private static void handleError(Context context, Throwable error, GeoReport geoReports[]) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);

        MobileMessagingLogger.e("Error reporting geo areas: " + error);

        mobileMessagingCore.setLastHttpException(error);
        mobileMessagingCore.getStats().reportError(MobileMessagingStatsError.GEO_REPORTING_ERROR);
        mobileMessagingCore.addUnreportedGeoEvents(geoReports);

        Intent reportingError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
        reportingError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(error));
        context.sendBroadcast(reportingError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(reportingError);
    }
}