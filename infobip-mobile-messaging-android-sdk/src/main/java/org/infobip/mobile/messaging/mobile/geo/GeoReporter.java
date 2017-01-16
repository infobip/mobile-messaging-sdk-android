package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporter {

    public void report(final Context context, final MobileMessagingStats stats) {

        final MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        final ArrayList<GeoReport> reports = mobileMessagingCore.removeUnreportedGeoEvents(context);
        if (reports.isEmpty() || !mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        new GeoReportingTask(context) {
            @Override
            protected void onPostExecute(GeoReportingResult result) {
                GeoReporter.handleSuccess(context, mobileMessagingCore, result, reports);
            }

            @Override
            protected void onCancelled(GeoReportingResult result) {
                GeoReporter.handleError(context, mobileMessagingCore, result.getError(), reports);
            }
        }.execute(reports.toArray(new GeoReport[reports.size()]));
    }

    public static void handleSuccess(Context context, MobileMessagingCore mobileMessagingCore, GeoReportingResult result, ArrayList<GeoReport> geoReports) {
        mobileMessagingCore.addCampaignStatus(result.getFinishedCampaignIds(), result.getSuspendedCampaignIds());

        List<String> finishedCampaignIds = new ArrayList<>();
        List<String> suspendedCampaignIds = new ArrayList<>();
        ArrayList<GeoReport> geoReportsToBroadcast = new ArrayList<>(geoReports);

        if (result.getFinishedCampaignIds() != null) {
            finishedCampaignIds = Arrays.asList(result.getFinishedCampaignIds());
        }
        if (result.getSuspendedCampaignIds() != null) {
            suspendedCampaignIds = Arrays.asList(result.getSuspendedCampaignIds());
        }

        if (!finishedCampaignIds.isEmpty() || !suspendedCampaignIds.isEmpty()) {
            for (GeoReport geoReport : geoReports) {
                if (finishedCampaignIds.contains(geoReport.getCampaignId()) || suspendedCampaignIds.contains(geoReport.getCampaignId())) {
                    geoReportsToBroadcast.remove(geoReport);
                }
            }
        }

        if (!geoReports.isEmpty()) {
            Intent geoReportsSent = new Intent(Event.GEOFENCE_EVENTS_REPORTED.getKey());
            geoReportsSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_GEOFENCE_REPORTS, geoReportsToBroadcast);
            context.sendBroadcast(geoReportsSent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(geoReportsSent);
        }
    }

    public static void handleError(Context context, MobileMessagingCore mobileMessagingCore, Throwable error, ArrayList<GeoReport> geoReports) {
        MobileMessagingCore.getInstance(context).getStats().reportError(MobileMessagingStatsError.GEO_REPORTING_ERROR);

        mobileMessagingCore.addUnreportedGeoEvents(geoReports);

        Intent seenStatusReportError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
        seenStatusReportError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(error));
        context.sendBroadcast(seenStatusReportError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(seenStatusReportError);
    }

    @NonNull
    public static EventReport[] prepareEventReport(GeoReport[] geoReports) {
        EventReport reports[] = new EventReport[geoReports.length];
        for (int i = 0; i < reports.length; i++) {

            Long timestampDelta = System.currentTimeMillis() - geoReports[i].getTimestampOccurred();
            Long timestampDeltaSeconds = TimeUnit.MILLISECONDS.toSeconds(timestampDelta);

            reports[i] = new EventReport(EventType.valueOf(geoReports[i].getEvent().name()), geoReports[i].getArea().getId(),
                    geoReports[i].getCampaignId(), geoReports[i].getMessageId(), timestampDeltaSeconds);
        }
        return reports;
    }
}
