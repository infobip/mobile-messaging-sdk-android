package org.infobip.mobile.messaging.geo.report;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoLatLng;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.transition.GeoNotificationHelper;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author sslavin
 * @since 07/02/2017.
 */

public class GeoReportHelper {

    /**
     * Returns signaling message for geofencing report
     *
     * @param messages all available messages
     * @param report   geofencing event report
     * @return corresponding signaling message
     */
    @Nullable
    public static Message getSignalingMessageForReport(List<Message> messages, GeoReport report) {
        for (Message m : messages) {
            if (!m.getMessageId().equals(report.getSignalingMessageId())) {
                continue;
            }
            return m;
        }
        return null;
    }

    /**
     * Creates new geo notification messages based on reporting result
     *
     * @param reportedEvents  events that were reported to server
     * @param reportingResult response from the server
     * @return map of messages and corresponding geo event types for each message
     */
    public static Map<Message, GeoEventType> createMessagesToNotify(Context context, List<GeoReport> reportedEvents, @NonNull GeoReportingResult reportingResult) {
        GeofencingHelper geofencingHelper = new GeofencingHelper(context);
        List<Message> allMessages = geofencingHelper.getMessageStoreForGeo().findAll(context);
        Map<Message, GeoEventType> messages = new ArrayMap<>();
        for (GeoReport report : reportedEvents) {
            Message signalingMessage = GeoReportHelper.getSignalingMessageForReport(allMessages, report);
            if (signalingMessage == null) {
                MobileMessagingLogger.e("Cannot find signaling message for id: " + report.getSignalingMessageId());
                continue;
            }

            messages.put(createNewMessageForReport(report, reportingResult, signalingMessage), report.getEvent());
        }
        return messages;
    }

    /**
     * Generates set of geofencing reports for multiple signaling messages (areas from multiple messages can be triggered at the same time).
     *
     * @param messagesAndAreas   map of signaling message and triggered geofence areas
     * @param event              transition type
     * @param triggeringLocation event location
     * @return list of geofencing reports for the provided messages and areas
     */
    public static GeoReport[] createReportsForMultipleMessages(Context context, Map<Message, List<Area>> messagesAndAreas, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        List<GeoReport> reports = new ArrayList<>();
        for (Message message : messagesAndAreas.keySet()) {
            List<Area> areas = messagesAndAreas.get(message);
            if (areas != null) {
                reports.addAll(createReports(context, message, areas, event, triggeringLocation));
            }
        }
        return reports.toArray(new GeoReport[0]);
    }

    /**
     * Generates set of geofencing reports
     *
     * @param context          context
     * @param signalingMessage original signaling message
     * @param areas            list of areas that triggered this geofencing event
     * @param event            transition type
     * @return set of geofencing reports to send to server
     */
    private static Set<GeoReport> createReports(Context context, Message signalingMessage, List<Area> areas, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        Set<GeoReport> reports = new ArraySet<>();
        for (Area area : areas) {
            GeoReport report = createReport(signalingMessage, area, event, triggeringLocation);
            reports.add(report);
            MobileMessagingCore.getInstance(context).addGeneratedMessageIds(report.getMessageId());
        }
        return reports;
    }

    /**
     * Generates geofencing event report
     *
     * @param signalingMessage original signaling push message with geofences
     * @param area             area that triggered the event
     * @param event            transition type
     * @return generated report with unique messageId or null if no report available for this geofence and transition
     */
    @NonNull
    private static GeoReport createReport(Message signalingMessage, Area area, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        Geo geo = GeoDataMapper.geoFromInternalData(signalingMessage.getInternalData());
        return new GeoReport(
                geo == null ? "" : geo.getCampaignId(),
                UUID.randomUUID().toString(),
                signalingMessage.getMessageId(),
                event,
                area,
                Time.now(),
                triggeringLocation
        );
    }

    /**
     * Creates new message based on geofencing report
     *
     * @param report          geofencing report for any supported event
     * @param reportingResult result of reporting geo events to server
     * @param originalMessage original signaling message
     * @return new message based on triggering event, area and original signaling message
     */
    private static Message createNewMessageForReport(@NonNull final GeoReport report, @NonNull GeoReportingResult reportingResult, @NonNull Message originalMessage) {
        GeoLatLng triggeringLocation = report.getTriggeringLocation();
        if (triggeringLocation == null) {
            triggeringLocation = new GeoLatLng(null, null);
        }

        List<Area> areas = new ArrayList<>();
        if (report.getArea() != null) {
            areas.add(report.getArea());
        }

        Geo geo;
        Geo originalMessageGeo = GeoDataMapper.geoFromInternalData(originalMessage.getInternalData());

        if (originalMessageGeo != null) {
            geo = new Geo(triggeringLocation.getLat(),
                    triggeringLocation.getLng(),
                    originalMessageGeo.getDeliveryTime(),
                    originalMessageGeo.getExpiryTime(),
                    originalMessageGeo.getStartTime(),
                    originalMessageGeo.getCampaignId(),
                    areas,
                    originalMessageGeo.getEvents(),
                    originalMessage.getSentTimestamp(),
                    originalMessage.getContentUrl());
        } else {
            geo = new Geo(triggeringLocation.getLat(),
                    triggeringLocation.getLng(),
                    null, null, null, null,
                    areas,
                    null,
                    Time.now(),
                    originalMessage.getContentUrl());
        }

        String internalData = GeoDataMapper.geoToInternalData(geo);
        return new Message(
                getMessageIdFromReport(report, reportingResult),
                originalMessage.getTitle(),
                originalMessage.getBody(),
                originalMessage.getSound(),
                originalMessage.isVibrate(),
                originalMessage.getIcon(),
                false, // enforcing non-silent
                originalMessage.getCategory(),
                originalMessage.getFrom(),
                Time.now(),
                0,
                Time.now(),
                originalMessage.getCustomPayload(),
                internalData,
                originalMessage.getDestination(),
                originalMessage.getStatus(),
                originalMessage.getStatusMessage(),
                originalMessage.getContentUrl(),
                originalMessage.getInAppStyle()
        );
    }

    /**
     * Retrieves message id based on reporting result (or based or report itself if result is not available now)
     *
     * @param report          geo event report to the server
     * @param reportingResult result of reporting
     * @return appropriate message id
     */
    private static String getMessageIdFromReport(@NonNull GeoReport report, @NonNull GeoReportingResult reportingResult) {
        if (reportingResult.getMessageIds() == null || reportingResult.getMessageIds().isEmpty()) {
            return report.getMessageId();
        }

        String newMessageId = reportingResult.getMessageIds().get(report.getMessageId());
        if (newMessageId == null) {
            return report.getMessageId();
        }

        return newMessageId;
    }

    /**
     * Returns list of inactive campaign ids based on reporting result
     *
     * @param result response from the server
     * @return set of inactive campaign ids
     */
    public static Set<String> getAndUpdateInactiveCampaigns(Context context, GeoReportingResult result) {
        Set<String> inactiveCampaigns = new ArraySet<>();
        if (result == null || result.hasError()) {
            inactiveCampaigns.addAll(GeofencingHelper.getSuspendedCampaignIds(context));
            inactiveCampaigns.addAll(GeofencingHelper.getFinishedCampaignIds(context));
            return inactiveCampaigns;
        }

        GeofencingHelper.addCampaignStatus(context, result.getFinishedCampaignIds(), result.getSuspendedCampaignIds());
        if (result.getFinishedCampaignIds() != null) {
            inactiveCampaigns.addAll(result.getFinishedCampaignIds());
        }
        if (result.getSuspendedCampaignIds() != null) {
            inactiveCampaigns.addAll(result.getSuspendedCampaignIds());
        }
        return inactiveCampaigns;
    }

    /**
     * Returns map of signaling messages and corresponding areas that match geofence and transition event
     *
     * @param messageStore message store to look messages for
     * @param requestIds   requestIds received from Google Location Services during transition event
     * @param event        transition event type
     * @return signaling messages with corresponding areas
     */
    @NonNull
    public static Map<Message, List<Area>> findSignalingMessagesAndAreas(Context context, MessageStore messageStore, Set<String> requestIds, @NonNull GeoEventType event) {
        Date now = Time.date();
        Map<Message, List<Area>> messagesAndAreas = new ArrayMap<>();
        for (Message message : messageStore.findAll(context)) {
            Geo geo = GeoDataMapper.geoFromInternalData(message.getInternalData());
            if (geo == null || geo.getAreasList() == null || geo.getAreasList().isEmpty()) {
                continue;
            }

            //don't trigger geo event before start date
            Date startDate = geo.getStartDate();
            if (startDate != null && startDate.after(now)) {
                continue;
            }

            List<Area> campaignAreas = geo.getAreasList();
            List<Area> triggeredAreas = new ArrayList<>();
            for (Area area : campaignAreas) {
                for (String requestId : requestIds) {
                    if (!requestId.equalsIgnoreCase(area.getId())) {
                        continue;
                    }

                    if (!GeoNotificationHelper.shouldReportTransition(context, geo, event)) {
                        continue;
                    }

                    triggeredAreas.add(area);
                }
            }

            if (!triggeredAreas.isEmpty()) {
                messagesAndAreas.put(message, triggeredAreas);
            }
        }

        return filterOverlappingAreas(messagesAndAreas);
    }

    /**
     * Filters out geo reports based on campaign status
     *
     * @param reports all reports sent to the server
     * @param result  result of reporting geofencing event reports to server
     * @return list of reports for active campaigns
     */
    public static List<GeoReport> filterOutNonActiveReports(Context context, @NonNull List<GeoReport> reports, @NonNull GeoReportingResult result) {
        if (result.hasError()) {
            return reports;
        }

        Set<String> inactiveCampaigns = getAndUpdateInactiveCampaigns(context, result);
        if (inactiveCampaigns.isEmpty()) {
            return reports;
        }

        List<GeoReport> activeReports = new ArrayList<>();
        for (GeoReport r : reports) {
            if (inactiveCampaigns.contains(r.getCampaignId())) {
                continue;
            }

            activeReports.add(r);
        }
        return activeReports;
    }

    /**
     * Filters out overlapping areas for each campaign and returns only the smallest area
     *
     * @param messagesAndAreas all triggered areas for each message
     * @return filtered areas
     */
    public static Map<Message, List<Area>> filterOverlappingAreas(Map<Message, List<Area>> messagesAndAreas) {
        Map<Message, List<Area>> filteredMessagesAndAreas = new ArrayMap<>(messagesAndAreas.size());

        for (Map.Entry<Message, List<Area>> entry : messagesAndAreas.entrySet()) {
            Message message = entry.getKey();
            List<Area> areasList = entry.getValue();

            if (areasList != null) {
                //using only area that has the smallest radius
                Collections.sort(areasList, new GeoAreaRadiusComparator());
                filteredMessagesAndAreas.put(message, Collections.singletonList(areasList.get(0)));
            }
        }

        return filteredMessagesAndAreas;
    }

    /**
     * Compares areas by radius
     */
    public static class GeoAreaRadiusComparator implements Comparator<Area> {

        @Override
        public int compare(Area area1, Area area2) {
            return area1.getRadius() - area2.getRadius();
        }
    }
}