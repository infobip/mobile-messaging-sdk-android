package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.geo.GeoAreasHandler;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.geo.GeoReportHelper;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.synchronizer.RetryableSynchronizer;
import org.infobip.mobile.messaging.mobile.synchronizer.Task;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;

import java.util.Arrays;
import java.util.List;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporter extends RetryableSynchronizer {

    private Broadcaster broadcaster;

    public GeoReporter(Context context, Broadcaster broadcaster, MobileMessagingStats stats) {
        super(context, stats);
        this.broadcaster = broadcaster;
    }

    @Override
    public void synchronize() {
        final MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        final GeoReport reports[] = mobileMessagingCore.removeUnreportedGeoEvents();
        if (reports.length == 0 || !mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        new GeoReportingTask(context) {

            protected void onPostExecute(GeoReportingResult result) {
                handleSuccess(context, reports, result);
                GeoAreasHandler.handleGeoReportingResult(context, result);
                if (result.hasError()) {
                    retry(result);
                }
            }

            protected void onCancelled(GeoReportingResult result) {
                handleError(context, result.getError(), reports);
                GeoAreasHandler.handleGeoReportingResult(context, result);
                retry(result);
            }
        }.execute(reports);
    }

    /**
     * Reports geo events synchronously
     * @param geoReports set that contains original messages and corresponding events
     * @return result that will contain lists of campaign ids and mappings between library generated message ids and IPCore ids
     */
    public @NonNull GeoReportingResult reportSync(@NonNull Context context, @NonNull GeoReport geoReports[]) {
        try {
            GeoReportingResult result = GeoReportingTask.executeSync(context, geoReports);
            handleSuccess(context, geoReports, result);
            return result;
        } catch (Exception e) {
            handleError(context, e, geoReports);
            return new GeoReportingResult(e);
        }
    }

    /**
     * Handles successful reporting of events to the server, will send out events with what was reported to the server.
     * @param geoReports reports that were sent to the server.
     * @param result result from the server
     */
    private void handleSuccess(Context context, GeoReport geoReports[], GeoReportingResult result) {
        List<GeoReport> geoReportsToBroadcast = GeoReportHelper.filterOutNonActiveReports(context, Arrays.asList(geoReports), result);
        broadcaster.geoReported(geoReportsToBroadcast);
    }

    /**
     * Handles any error that happens during reporting to the server.
     * @param error error that happens
     * @param geoReports reports sent to server
     */
    private void handleError(Context context, Throwable error, GeoReport geoReports[]) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);

        MobileMessagingLogger.e("Error reporting geo areas: " + error);

        mobileMessagingCore.setLastHttpException(error);
        mobileMessagingCore.getStats().reportError(MobileMessagingStatsError.GEO_REPORTING_ERROR);
        mobileMessagingCore.addUnreportedGeoEvents(geoReports);

        broadcaster.error(MobileMessagingError.createFrom(error));
    }

    @Override
    public Task getTask() {
        return Task.GEO_REPORT;
    }
}