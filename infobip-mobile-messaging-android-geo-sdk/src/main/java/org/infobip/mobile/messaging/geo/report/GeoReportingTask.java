package org.infobip.mobile.messaging.geo.report;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.api.geo.MessagePayload;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 20/10/2016.
 */
class GeoReportingTask extends AsyncTask<GeoReport, Void, GeoReportingResult> {

    private final Context context;
    private final GeofencingHelper geofenceHelper;

    GeoReportingTask(Context context, GeofencingHelper geofenceHelper) {
        this.context = context;
        this.geofenceHelper = geofenceHelper;
    }

    /**
     * Sends geo events synchronously to the server
     *
     * @param geoReports reports to send
     * @return response from the server
     * @throws RuntimeException if cannot process geo reports
     */
    @NonNull
    static GeoReportingResult executeSync(Context context, GeofencingHelper geofenceHelper, GeoReport geoReports[]) throws RuntimeException {
        EventReportBody eventReportBody = prepareEventReportBody(context, geofenceHelper.getMessageStoreForGeo(),  geoReports);
        EventReportResponse eventResponse = MobileApiResourceProvider.INSTANCE.getMobileApiGeo(context).report(eventReportBody);
        return new GeoReportingResult(eventResponse);
    }

    @Override
    protected GeoReportingResult doInBackground(GeoReport geoReports[]) {
        if (geoReports.length < 1) {
            return new GeoReportingResult(new IllegalArgumentException("No geo reports to send"));
        }

        try {
            return executeSync(context, geofenceHelper, geoReports);
        } catch (Exception e) {
            MobileMessagingLogger.e("Error reporting geo areas!", e);
            cancel(true);

            return new GeoReportingResult(e);
        }
    }

    /**
     * Creates event report request body based on provided geofencing report.
     *
     *
     * @param geoMessageStore
     * @param geoReports map that contains original signaling messages as keys and related geo reports as values.
     * @return request body for geo reporting.
     */
    @NonNull
    private static EventReportBody prepareEventReportBody(Context context, MessageStore geoMessageStore, @NonNull GeoReport geoReports[]) {
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

            Long timestampDelta = Time.now() - r.getTimestampOccurred();
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

        String deviceInstanceId = MobileMessagingCore.getInstance(context).getDeviceApplicationInstanceId();
        return new EventReportBody(messagePayloads, eventReports, deviceInstanceId);
    }
}