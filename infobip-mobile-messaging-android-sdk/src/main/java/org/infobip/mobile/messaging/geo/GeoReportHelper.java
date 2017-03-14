package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.geo.GeoReportingResult;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
     * @param messages all available messages
     * @param report geofencing event report
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
     * @param reportedEvents events that were reported to server
     * @param reportingResult response from the server
     * @return map of messages and corresponding geo event types for each message
     */
    static Map<Message, GeoEventType> createMessagesToNotify(Context context, List<GeoReport> reportedEvents, @NonNull GeoReportingResult reportingResult) {
        List<Message> allMessages = MobileMessagingCore.getInstance(context).getMessageStoreForGeo().findAll(context);
        Map<Message, GeoEventType> messages = new HashMap<>();
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
     * @param messagesAndAreas map of signaling message and triggered geofence areas
     * @param event transition type
     * @param triggeringLocation event location
     * @return list of geofencing reports for the provided messages and areas
     */
    static GeoReport[] createReportsForMultipleMessages(Map<Message, List<Area>> messagesAndAreas, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        List<GeoReport> reports = new ArrayList<>();
        for (Message message : messagesAndAreas.keySet()) {
            reports.addAll(createReports(message, messagesAndAreas.get(message), event, triggeringLocation));
        }
        return reports.toArray(new GeoReport[reports.size()]);
    }

    /**
     * Generates set of geofencing reports
     * @param signalingMessage original signaling message
     * @param areas list of areas that triggered this geofencing event
     * @param event transition type
     * @return set of geofencing reports to send to server
     */
    private static Set<GeoReport> createReports(Message signalingMessage, List<Area> areas, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        Set<GeoReport> reports = new HashSet<>();
        for (Area area : areas) {
            reports.add(
                    createReport(signalingMessage, area, event, triggeringLocation)
            );
        }
        return reports;
    }

    /**
     * Generates geofencing event report
     * @param signalingMessage original signaling push message with geofences
     * @param area area that triggered the event
     * @param event transition type
     * @return generated report with unique messageId or null if no report available for this geofence and transition
     */
    private static @NonNull GeoReport createReport(Message signalingMessage, Area area, @NonNull GeoEventType event, @NonNull GeoLatLng triggeringLocation) {
        return new GeoReport(
                signalingMessage.getGeo().getCampaignId(),
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
     * @param report geofencing report for any supported event
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
        if (originalMessage.getGeo() != null) {
            geo = new Geo(triggeringLocation.getLat(),
                    triggeringLocation.getLng(),
                    originalMessage.getGeo().getDeliveryTime(),
                    originalMessage.getGeo().getExpiryTime(),
                    originalMessage.getGeo().getStartTime(),
                    originalMessage.getGeo().getCampaignId(),
                    areas,
                    originalMessage.getGeo().getEvents());
        } else {
            geo = new Geo(triggeringLocation.getLat(),
                    triggeringLocation.getLng(),
                    null, null, null, null,
                    areas,
                    null);
        }

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
                originalMessage.getCustomPayload(),
                geo,
                originalMessage.getDestination(),
                originalMessage.getStatus(),
                originalMessage.getStatusMessage()
        );
    }

    /**
     * Retrieves message id based on reporting result (or based or report itself if result is not available now)
     * @param report geo event report to the server
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
     * @param result response from the server
     * @return set of inactive campaign ids
     */
    public static Set<String> getAndUpdateInactiveCampaigns(MobileMessagingCore mobileMessagingCore, GeoReportingResult result) {
        Set<String> inactiveCampaigns = new HashSet<>();
        if (result == null || result.hasError()) {
            inactiveCampaigns.addAll(mobileMessagingCore.getSuspendedCampaignIds());
            inactiveCampaigns.addAll(mobileMessagingCore.getFinishedCampaignIds());
            return inactiveCampaigns;
        }

        mobileMessagingCore.addCampaignStatus(result.getFinishedCampaignIds(), result.getSuspendedCampaignIds());
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
     * @param messageStore message store to look messages for
     * @param requestIds requestIds received from Google Location Services during transition event
     * @param event transition event type
     * @return signaling messages with corresponding areas
     */
    @NonNull
    static Map<Message, List<Area>> findSignalingMessagesAndAreas(Context context, MessageStore messageStore, Set<String> requestIds, @NonNull GeoEventType event) {
        Date now = new Date();
        Map<Message, List<Area>> messagesAndAreas = new HashMap<>();
        for (Message message : messageStore.findAll(context)) {
            Geo geo = message.getGeo();
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

                    if (!GeoNotificationHelper.shouldReportTransition(context, message, event)) {
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
     * @param reports all reports sent to the server
     * @param result result of reporting geofencing event reports to server
     * @return list of reports for active campaigns
     */
    public static List<GeoReport> filterOutNonActiveReports(Context context, @NonNull List<GeoReport> reports, @NonNull GeoReportingResult result) {
        if (result.hasError()) {
            return reports;
        }

        Set<String> inactiveCampaigns = getAndUpdateInactiveCampaigns(MobileMessagingCore.getInstance(context), result);
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
     * @param messagesAndAreas all triggered areas for each message
     * @return filteted areas
     */
    static private Map<Message, List<Area>> filterOverlappingAreas(Map<Message, List<Area>> messagesAndAreas) {
        Map<Message, List<Area>> filteredMessagesAndAreas = new HashMap<>(messagesAndAreas.size());

        for (Message message : messagesAndAreas.keySet()) {
            List<Area> areasList = message.getGeo().getAreasList();

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
    static class GeoAreaRadiusComparator implements Comparator<Area> {

        @Override
        public int compare(Area area1, Area area2) {
            return area1.getRadius() - area2.getRadius();
        }
    }
}