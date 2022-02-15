package org.infobip.mobile.messaging.geo.report;

import android.content.Context;
import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.api.geo.MessagePayload;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.platform.GeoBroadcaster;
import org.infobip.mobile.messaging.geo.transition.GeoAreasHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporter {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final GeoBroadcaster broadcaster;
    private final GeofencingHelper geofenceHelper;
    private final MobileApiGeo mobileApiGeo;
    private final MRetryPolicy retryPolicy;

    public GeoReporter(Context context, MobileMessagingCore mobileMessagingCore, GeoBroadcaster broadcaster, MobileMessagingStats stats, MobileApiGeo mobileApiGeo) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.broadcaster = broadcaster;
        this.geofenceHelper = new GeofencingHelper(context);
        this.mobileApiGeo = mobileApiGeo;
        this.retryPolicy = new RetryPolicyProvider(context).DEFAULT();
    }

    public void synchronize() {
        final GeoReport[] reports = geofenceHelper.removeUnreportedGeoEvents();
        if (reports.length == 0 || !mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        new MRetryableTask<GeoReport, GeoReportingResult>() {
            @Override
            public GeoReportingResult run(GeoReport[] reports) {
                return reportSync(reports);
            }

            @Override
            public void after(GeoReportingResult geoReportingResult) {
                handleSuccess(context, reports, geoReportingResult);
                GeoAreasHandler.handleGeoReportingResult(context, geoReportingResult);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("Error reporting geo areas!", error);
                handleError(context, error, reports);
                GeoAreasHandler.handleGeoReportingResult(context, new GeoReportingResult(error));
            }
        }
        .retryWith(retryPolicy)
        .execute(reports);
    }

    /**
     * Reports geo events synchronously
     *
     * @param geoReports set that contains original messages and corresponding events
     * @return result that will contain lists of campaign ids and mappings between library generated message ids and IPCore ids
     */
    @NonNull
    public GeoReportingResult reportSync(@NonNull GeoReport[] geoReports) {
        EventReportBody eventReportBody = prepareEventReportBody(context, geofenceHelper.getMessageStoreForGeo(), geoReports);
        MobileMessagingLogger.v("GEO REPORT >>>", eventReportBody);
        EventReportResponse eventResponse = mobileApiGeo.report(eventReportBody);
        MobileMessagingLogger.v("GEO REPORT DONE <<<", eventResponse);
        GeoReportingResult result = new GeoReportingResult(eventResponse);
        handleSuccess(context, geoReports, result);
        return result;
    }

    /**
     * Handles successful reporting of events to the server, will send out events with what was reported to the server.
     *
     * @param geoReports reports that were sent to the server.
     * @param result     result from the server
     */
    private void handleSuccess(Context context, GeoReport[] geoReports, GeoReportingResult result) {
        List<GeoReport> geoReportsToBroadcast = GeoReportHelper.filterOutNonActiveReports(context, Arrays.asList(geoReports), result);
        broadcaster.geoReported(geoReportsToBroadcast);
    }

    /**
     * Handles any error that happens during reporting to the server.
     *
     * @param error      error that happens
     * @param geoReports reports sent to server
     */
    private void handleError(Context context, Throwable error, GeoReport[] geoReports) {
        MobileMessagingLogger.e("Error reporting geo areas: " + error);
        stats.reportError(MobileMessagingStatsError.GEO_REPORTING_ERROR);

        geofenceHelper.addUnreportedGeoEvents(geoReports);
        broadcaster.error(MobileMessagingError.createFrom(error));
    }

    /**
     * Creates event report request body based on provided geofencing report.
     *
     * @param geoMessageStore message store for geo
     * @param geoReports      map that contains original signaling messages as keys and related geo reports as values.
     * @return request body for geo reporting.
     */
    @NonNull
    private static EventReportBody prepareEventReportBody(Context context, MessageStore geoMessageStore, @NonNull GeoReport[] geoReports) {
        Set<MessagePayload> messagePayloads = new HashSet<>();
        Set<EventReport> eventReports = new HashSet<>();

        List<Message> messages = geoMessageStore.findAll(context);

        for (GeoReport r : geoReports) {

            Message m = GeoReportHelper.getSignalingMessageForReport(messages, r);
            if (m == null) {
                MobileMessagingLogger.e("Cannot find signaling message for id: " + r.getSignalingMessageId());
                continue;
            }

            messagePayloads.add(new MessagePayload(
                    m.getMessageId(),
                    m.getTitle(),
                    m.getBody(),
                    m.getSound(),
                    m.isVibrate(),
                    m.getCategory(),
                    m.isSilent(),
                    m.getCustomPayload() != null ? m.getCustomPayload().toString() : null,
                    InternalDataMapper.createInternalDataBasedOnMessageContents(m)
            ));

            long timestampDelta = Time.now() - r.getTimestampOccurred();
            Long timestampDeltaSeconds = TimeUnit.MILLISECONDS.toSeconds(timestampDelta);

            eventReports.add(new EventReport(
                    EventType.valueOf(r.getEvent().name()),
                    r.getArea().getId(),
                    r.getCampaignId(),
                    r.getSignalingMessageId(),
                    r.getMessageId(),
                    timestampDeltaSeconds
            ));
        }

        String deviceInstanceId = MobileMessagingCore.getInstance(context).getPushRegistrationId();
        return new EventReportBody(messagePayloads, eventReports, deviceInstanceId);
    }
}