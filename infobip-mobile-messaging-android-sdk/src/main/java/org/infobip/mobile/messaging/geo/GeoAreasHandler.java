package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.SparseArray;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReports;
import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.geo.GeoReportingResult;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

/**
 * @author pandric
 * @since 24.06.2016.
 */
class GeoAreasHandler {

    private static final String TAG = "GeofenceTransitions";
    private static final String AREA_NOTIFIED_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.notified.";
    private static final String AREA_LAST_TIME_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.last.time.";
    private static final GeoEvent DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER = new GeoEvent(GeoEventType.entry, 1, 0L);

    private final MessageStore messageStore;
    private final Context context;

    private static SparseArray<String> transitionNames = new SparseArray<String>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, "GEOFENCE_TRANSITION_ENTER");
        put(Geofence.GEOFENCE_TRANSITION_EXIT, "GEOFENCE_TRANSITION_EXIT");
        put(Geofence.GEOFENCE_TRANSITION_DWELL, "GEOFENCE_TRANSITION_DWELL");
    }};

    private static SparseArray<String> geofencingErrors = new SparseArray<String>() {{
        put(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE, "Geofence not available");
        put(GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES, "Too many geofences");
        put(GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS, "Too many pending intents");
    }};

    private static SparseArray<Event> transitionBroadcasts = new SparseArray<Event>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, Event.GEOFENCE_AREA_ENTERED);
    }};

    private static SparseArray<GeoEventType> transitionReportEvents = new SparseArray<GeoEventType>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, GeoEventType.entry);
    }};

    GeoAreasHandler(Context context) {
        this.context = context;
        this.messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
    }

    void handleTransition(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) return;

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            MobileMessagingLogger.e(TAG, "ERROR:" + geofencingErrors.get(errorCode));

            if (GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE == errorCode) {
                MobileMessagingCore.getInstance(context).setAllActiveGeoAreasMonitored(false);
            }
            return;
        }

        handleTransition(geofencingEvent.getTriggeringGeofences(),
                geofencingEvent.getGeofenceTransition(),
                geofencingEvent.getTriggeringLocation());
    }

    void handleTransition(List<Geofence> triggeringGeofences, int geofenceTransition, Location triggeringLocation) {

        Map<Message, List<Area>> messagesAndAreas = findActiveMessagesAndAreasForTriggeredGeofences(context, triggeringGeofences);
        Map<Message, List<Area>> filteredMessagesAndAreas = filterOverlappingAreas(messagesAndAreas);

        if (filteredMessagesAndAreas == null || filteredMessagesAndAreas.isEmpty()) {
            return;
        }

        Pair<ArrayList<GeoReport>, ArrayList<Pair<Geo, Message>>> geoReportsAndDataToNotify =
                prepareGeoReportsAndDataToNotify(geofenceTransition, triggeringLocation, filteredMessagesAndAreas);

        ArrayList<GeoReport> geoReports = geoReportsAndDataToNotify.first;
        ArrayList<Pair<Geo, Message>> geoDataToNotify = geoReportsAndDataToNotify.second;

        if (!geoReports.isEmpty() && !geoDataToNotify.isEmpty()) {
            MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
            EventReport reports[] = GeoReporter.prepareEventReport(geoReports.toArray(new GeoReport[geoReports.size()]));

            List<String> finishedCampaignIds = Arrays.asList(mobileMessagingCore.getFinishedCampaignIds());
            List<String> suspendedCampaignIds = Arrays.asList(mobileMessagingCore.getSuspendedCampaignIds());

            //send reports
            try {
                final GeoReportingResult result = new GeoReportingResult(MobileApiResourceProvider.INSTANCE.getMobileApiGeo(context)
                        .report(new EventReports(reports)));

                if (result.getFinishedCampaignIds() != null) {
                    finishedCampaignIds = Arrays.asList(result.getFinishedCampaignIds());
                }
                if (result.getSuspendedCampaignIds() != null) {
                    suspendedCampaignIds = Arrays.asList(result.getSuspendedCampaignIds());
                }

                GeoReporter.handleSuccess(context, mobileMessagingCore, result, geoReports);

            } catch (Exception e) {
                GeoReporter.handleError(context, mobileMessagingCore, e, geoReports);
            }

            //display notifications
            displayNotificationsForActiveCampaigns(geoDataToNotify, geofenceTransition, finishedCampaignIds, suspendedCampaignIds);
        }

    }

    private Pair<ArrayList<GeoReport>, ArrayList<Pair<Geo, Message>>> prepareGeoReportsAndDataToNotify(int geofenceTransition,
                                                                                                       Location triggeringLocation,
                                                                                                       Map<Message, List<Area>> filteredMessagesAndAreas) {
        ArrayList<GeoReport> geoReports = new ArrayList<>(filteredMessagesAndAreas.size());
        ArrayList<Pair<Geo, Message>> geoDataToNotify = new ArrayList<>(filteredMessagesAndAreas.size());

        for (Message message : filteredMessagesAndAreas.keySet()) {

            List<Area> geofenceAreasList = filteredMessagesAndAreas.get(message);
            logGeofences(geofenceAreasList, geofenceTransition);

            for (final Area area : geofenceAreasList) {

                if (!shouldReportTransition(message, geofenceTransition)) {
                    continue;
                }

                Geo geoToReportAndNotify = new Geo(
                        triggeringLocation.getLatitude(),
                        triggeringLocation.getLongitude(),
                        new ArrayList<Area>() {{
                            add(area);
                        }},
                        message.getGeo().getDeliveryTime(),
                        null,
                        message.getGeo().getExpiryDate().toString(),
                        message.getGeo().getStartDate().toString(),
                        message.getGeo().getCampaignId());


                geoDataToNotify.add(new Pair<>(geoToReportAndNotify, message));

                geoReports.add(new GeoReport(message.getGeo().getCampaignId(), message.getMessageId(),
                        transitionReportEvents.get(geofenceTransition), area, System.currentTimeMillis()));
            }
        }

        return new Pair<>(geoReports, geoDataToNotify);
    }

    private void displayNotificationsForActiveCampaigns(ArrayList<Pair<Geo, Message>> geoDataToNotify, int geofenceTransition, List<String> finishedCampaignIds, List<String> suspendedCampaignIds) {
        for (Pair<Geo, Message> geoData : geoDataToNotify) {
            final Geo geo = geoData.first;
            final Message message = geoData.second;
            if (finishedCampaignIds.contains(geo.getCampaignId()) || suspendedCampaignIds.contains(geo.getCampaignId())) {
                continue;
            }

            setLastNotificationTimeForArea(geo.getCampaignId(), geofenceTransition, System.currentTimeMillis());
            setNumberOfDisplayedNotificationsForArea(geo.getCampaignId(), geofenceTransition,
                    getNumberOfDisplayedNotificationsForArea(geo.getCampaignId(), geofenceTransition) + 1);

            notifyAboutTransition(context, geo, geofenceTransition, message);
        }
    }

    private boolean shouldReportTransition(Message message, int geofenceTransition) {
        int numberOfDisplayedNotifications = getNumberOfDisplayedNotificationsForArea(message.getGeo().getCampaignId(), geofenceTransition);
        long lastNotificationTimeForArea = getLastNotificationTimeForArea(message.getGeo().getCampaignId(), geofenceTransition);
        Geo geo = message.getGeo();
        GeoEvent settings = getNotificationSettingsForTransition(geo.getEvents(), geofenceTransition);

        boolean isInDeliveryWindow = checkIsAreaInDeliveryWindow(geo.getDeliveryTime());

        return settings != null &&
                isInDeliveryWindow &&
                (settings.getLimit() > numberOfDisplayedNotifications || settings.getLimit() == GeoEvent.UNLIMITED_RECURRING) &&
                TimeUnit.MINUTES.toMillis(settings.getTimeoutInMinutes()) < System.currentTimeMillis() - lastNotificationTimeForArea &&
                geoEventMatchesTransition(settings, geofenceTransition) &&
                !geo.isExpired();
    }

    private boolean checkIsAreaInDeliveryWindow(DeliveryTime deliveryTime) {
        try {
            if (deliveryTime == null) {
                return true;
            }

            String daysPayload = deliveryTime.getDays();
            if (!shouldDeliverToday(daysPayload)) {
                return false;
            }

            String timeInterval = deliveryTime.getTimeInterval();
            return checkIsDeliveryInTimeInterval(timeInterval);

        } catch (ParseException e) {
            MobileMessagingLogger.e(e.getMessage(), e);
            return true;
        }
    }

    private boolean shouldDeliverToday(String daysPayload) {
        String[] days = null;

        if (daysPayload == null) {
            return false;
        }

        try {
            days = daysPayload.split(",");
        } catch (PatternSyntaxException e) {
            MobileMessagingLogger.e(e.getMessage(), e);
        }

        if (days == null) {
            return false;
        }

        int dayOfMonthISO8601 = DateTimeUtil.dayOfWeekISO8601();

        for (String day : days) {
            if (day.equalsIgnoreCase(String.valueOf(dayOfMonthISO8601))) {
                return true;
            }
        }

        return false;
    }

    private boolean checkIsDeliveryInTimeInterval(String timeInterval) throws ParseException {
        if (timeInterval == null) {
            return false;
        }

        String[] timeIntervalStartEnd = timeInterval.split("/");
        String startTime = timeIntervalStartEnd[0];
        String endTime = timeIntervalStartEnd[1];

        return DateTimeUtil.isCurrentTimeBetweenDates(startTime, endTime);
    }

    private GeoEvent getNotificationSettingsForTransition(List<GeoEvent> eventFilters, int geofenceTransition) {
        if (eventFilters == null || eventFilters.isEmpty()) {
            return DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER;
        }

        for (GeoEvent e : eventFilters) {
            if (!geoEventMatchesTransition(e, geofenceTransition)) {
                continue;
            }
            return e;
        }
        return null;
    }

    private String areaNotificationNumKey(String campaignId, int geofenceTransition) {
        return AREA_NOTIFIED_PREF_PREFIX + campaignId + "-" + geofenceTransition;
    }

    private String areaNotificationTimeKey(String campaignId, int geofenceTransition) {
        return AREA_LAST_TIME_PREF_PREFIX + campaignId + "-" + geofenceTransition;
    }

    private boolean geoEventMatchesTransition(GeoEvent event, int geofenceTransition) {
        return event.getType().equals(DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER.getType()) && geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER;
    }

    private int getNumberOfDisplayedNotificationsForArea(String campaignId, int geofenceTransition) {
        return PreferenceHelper.findInt(context, areaNotificationNumKey(campaignId, geofenceTransition), 0);
    }

    private void setNumberOfDisplayedNotificationsForArea(String campaignId, int geofenceTransition, int n) {
        PreferenceHelper.saveInt(context, areaNotificationNumKey(campaignId, geofenceTransition), n);
    }

    private long getLastNotificationTimeForArea(String campaignId, int geofenceTransition) {
        return PreferenceHelper.findLong(context, areaNotificationTimeKey(campaignId, geofenceTransition), 0);
    }

    private void setLastNotificationTimeForArea(String campaignId, int geofenceTransition, long timeMs) {
        PreferenceHelper.saveLong(context, areaNotificationTimeKey(campaignId, geofenceTransition), timeMs);
    }

    private void notifyAboutTransition(Context context, Geo geo, int geofenceTransition, Message message) {
        saveMessage(message);
        NotificationHandler.displayNotification(context, message);
        sendGeoEventBroadcast(context, geofenceTransition, geo, message);
    }

    private void saveMessage(Message message) {
        if (!MobileMessagingCore.getInstance(context).isMessageStoreEnabled()) {
            return;
        }

        message.setSeenTimestamp(0);
        message.setSilent(false);
        message.setReceivedTimestamp(System.currentTimeMillis());
        MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
    }

    private void sendGeoEventBroadcast(Context context, int geofenceTransition, Geo geo, Message message) {
        Event event = transitionBroadcasts.get(geofenceTransition);
        if (event == null) {
            return;
        }

        Intent geofenceIntent = new Intent(event.getKey());
        geofenceIntent.putExtra(BroadcastParameter.EXTRA_GEOFENCE_AREAS, geo);
        geofenceIntent.putExtras(BundleMessageMapper.toBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceIntent);
        context.sendBroadcast(geofenceIntent);

        Intent messageIntent = new Intent(Event.MESSAGE_RECEIVED.getKey());
        messageIntent.putExtras(BundleMessageMapper.toBundle(message));
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
        context.sendBroadcast(messageIntent);
    }

    private void logGeofences(List<Area> areas, int transition) {
        for (Area a : areas) {
            MobileMessagingLogger.i(TAG, transitionNames.get(transition) + " (" + a.getTitle() + ") LAT:" + a.getLatitude() + " LON:" + a.getLongitude() + " RAD:" + a.getRadius());
        }
    }

    private Map<Message, List<Area>> findActiveMessagesAndAreasForTriggeredGeofences(Context context, List<Geofence> geofences) {
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
                for (Geofence geofence : geofences) {
                    if (geofence.getRequestId().equalsIgnoreCase(area.getId())) {
                        triggeredAreas.add(area);
                    }
                }
            }

            if (!triggeredAreas.isEmpty()) {
                messagesAndAreas.put(message, triggeredAreas);
            }
        }

        return messagesAndAreas;
    }

    private Map<Message, List<Area>> filterOverlappingAreas(Map<Message, List<Area>> messagesAndAreas) {
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


    static class GeoAreaRadiusComparator implements Comparator<Area> {

        @Override
        public int compare(Area area1, Area area2) {
            return area1.getRadius() - area2.getRadius();
        }
    }

}