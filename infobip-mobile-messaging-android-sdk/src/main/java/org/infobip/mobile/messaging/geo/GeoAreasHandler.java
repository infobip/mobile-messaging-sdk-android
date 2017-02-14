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
            MobileMessagingLogger.e(TAG, "ERROR:" + geofencingErrors.get(geofencingEvent.getErrorCode()));
            return;
        }

        handleTransition(geofencingEvent.getTriggeringGeofences(),
                geofencingEvent.getGeofenceTransition(),
                geofencingEvent.getTriggeringLocation());
    }

    void handleTransition(List<Geofence> triggeringGeofences, int geofenceTransition, Location triggeringLocation) {

        Map<Message, List<Area>> messagesAndAreas = findMessagesAndAreasForTriggeringGeofences(
                context, triggeringGeofences);
        if (messagesAndAreas == null || messagesAndAreas.isEmpty()) {
            return;
        }

        ArrayList<GeoReport> geoReports = new ArrayList<>();
        ArrayList<Pair<Geo, Message>> geoDataToNotify = new ArrayList<>();

        for (Message message : messagesAndAreas.keySet()) {

            List<Area> geofenceAreasList = messagesAndAreas.get(message);
            logGeofences(geofenceAreasList, geofenceTransition);

            for (final Area area : geofenceAreasList) {

                if (!shouldReportTransition(message, area, geofenceTransition)) {
                    continue;
                }

                Geo geoToReportAndNotify = new Geo(
                        triggeringLocation.getLatitude(),
                        triggeringLocation.getLongitude(),
                        new ArrayList<Area>() {{
                            add(area);
                        }});

                geoDataToNotify.add(new Pair<>(geoToReportAndNotify, message));

                geoReports.add(new GeoReport(message.getGeo().getCampaignId(), message.getMessageId(),
                        transitionReportEvents.get(geofenceTransition), area, System.currentTimeMillis()));
            }
        }

        if (!geoReports.isEmpty() && !geoDataToNotify.isEmpty()) {
            MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
            EventReport reports[] = GeoReporter.prepareEventReport(geoReports.toArray(new GeoReport[geoReports.size()]));

            List<String> finishedCampaignIds = Arrays.asList(mobileMessagingCore.getFinishedCampaignIds());
            List<String> suspendedCampaignIds = Arrays.asList(mobileMessagingCore.getSuspendedCampaignIds());

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

            displayNotificationsForActiveCampaigns(geoDataToNotify, geofenceTransition, finishedCampaignIds, suspendedCampaignIds);
        }

    }

    private void displayNotificationsForActiveCampaigns(ArrayList<Pair<Geo, Message>> geoDataToNotify, int geofenceTransition, List<String> finishedCampaignIds, List<String> suspendedCampaignIds) {
        for (Pair<Geo, Message> geoData : geoDataToNotify) {
            final Geo geo = geoData.first;
            final Message message = geoData.second;
            if (finishedCampaignIds.contains(message.getGeo().getCampaignId()) || suspendedCampaignIds.contains(message.getGeo().getCampaignId())) {
                continue;
            }

            for (Area area : geo.getAreasList()) {
                setLastNotificationTimeForArea(message.getMessageId(), area.getId(), geofenceTransition, System.currentTimeMillis());
                setNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition,
                        getNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition) + 1);
            }

            notifyAboutTransition(context, geo, geofenceTransition, geoData.second);
        }
    }

    private boolean shouldReportTransition(Message message, Area area, int geofenceTransition) {
        int numberOfDisplayedNotifications = getNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition);
        long lastNotificationTimeForArea = getLastNotificationTimeForArea(message.getMessageId(), area.getId(), geofenceTransition);
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

    private String areaNotificationNumKey(String messageId, String areaId, int geofenceTransition) {
        return AREA_NOTIFIED_PREF_PREFIX + messageId + "-" + areaId + "-" + geofenceTransition;
    }

    private String areaNotificationTimeKey(String messageId, String areaId, int geofenceTransition) {
        return AREA_LAST_TIME_PREF_PREFIX + messageId + "-" + areaId + "-" + geofenceTransition;
    }

    private boolean geoEventMatchesTransition(GeoEvent event, int geofenceTransition) {
        return event.getType().equals(DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER.getType()) && geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER;
    }

    private int getNumberOfDisplayedNotificationsForArea(String messageId, String areaId, int geofenceTransition) {
        return PreferenceHelper.findInt(context, areaNotificationNumKey(messageId, areaId, geofenceTransition), 0);
    }

    private void setNumberOfDisplayedNotificationsForArea(String messageId, String areaId, int geofenceTransition, int n) {
        PreferenceHelper.saveInt(context, areaNotificationNumKey(messageId, areaId, geofenceTransition), n);
    }

    private long getLastNotificationTimeForArea(String messageId, String areaId, int geofenceTransition) {
        return PreferenceHelper.findLong(context, areaNotificationTimeKey(messageId, areaId, geofenceTransition), 0);
    }

    private void setLastNotificationTimeForArea(String messageId, String areaId, int geofenceTransition, long timeMs) {
        PreferenceHelper.saveLong(context, areaNotificationTimeKey(messageId, areaId, geofenceTransition), timeMs);
    }

    private void notifyAboutTransition(Context context, Geo geo, int geofenceTransition, Message message) {
        NotificationHandler.displayNotification(context, message);
        sendGeoEventBroadcast(context, geofenceTransition, geo, message);
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

    private Map<Message, List<Area>> findMessagesAndAreasForTriggeringGeofences(Context context, List<Geofence> geofences) {
        Map<Message, List<Area>> messagesAndAreas = new HashMap<>();
        for (Message message : messageStore.findAll(context)) {
            Geo geo = message.getGeo();
            if (geo == null || geo.getAreasList() == null || geo.getAreasList().isEmpty()) {
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
}