package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.GeofenceAreas;
import org.infobip.mobile.messaging.GeofenceAreas.Area;
import org.infobip.mobile.messaging.GeofenceAreas.Area.GeoEvent;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final GeoEvent DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER = new GeoEvent("entry", 1, 0L);

    private final MessageStore messageStore = new SharedPreferencesMessageStore();
    private final Random random = new Random();
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

    private static SparseArray<Event> transitionEvents = new SparseArray<Event>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, Event.GEOFENCE_AREA_ENTERED);
    }};

    GeoAreasHandler(Context context) {
        this.context = context;
    }

    void handleTransition(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) return;

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "ERROR:" + geofencingErrors.get(geofencingEvent.getErrorCode()));
            return;
        }

        Map<Message, List<Area>> messagesAndAreas = findMessagesAndAreasForTriggeringGeofences(
                context, geofencingEvent.getTriggeringGeofences());
        if (messagesAndAreas == null || messagesAndAreas.isEmpty()) {
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        for (Message message : messagesAndAreas.keySet()) {

            List<Area> geofenceAreasList = messagesAndAreas.get(message);
            logGeofences(geofenceAreasList, geofenceTransition);

            for (final Area area : geofenceAreasList) {

                if (!shouldNotifyAboutTransition(message, area, geofenceTransition)) {
                    continue;
                }

                Location triggeringLocation = geofencingEvent.getTriggeringLocation();
                GeofenceAreas geofenceAreas = new GeofenceAreas(
                        triggeringLocation.getLatitude(),
                        triggeringLocation.getLongitude(),
                        new ArrayList<Area>() {{
                            add(area);
                        }});

                notifyAboutTransition(context, geofenceAreas, geofenceTransition, message);

                setLastNotificationTimeForArea(message.getMessageId(), area.getId(), geofenceTransition, System.currentTimeMillis());
                setNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition,
                        getNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition) + 1);
            }
        }
    }

    private boolean shouldNotifyAboutTransition(Message message, Area area, int geofenceTransition) {
        int numberOfDisplayedNotifications = getNumberOfDisplayedNotificationsForArea(message.getMessageId(), area.getId(), geofenceTransition);
        long lastNotificationTimeForArea = getLastNotificationTimeForArea(message.getMessageId(), area.getId(), geofenceTransition);
        GeoEvent settings = getNotificationSettingsForTransition(area, geofenceTransition);

        boolean isInDeliveryWindow = checkIsAreaInDeliveryWindow(area.getDeliveryTime());

        return settings != null &&
                isInDeliveryWindow &&
                (settings.getLimit() > numberOfDisplayedNotifications || settings.getLimit() == GeoEvent.UNLIMITED_RECURRING) &&
                TimeUnit.MINUTES.toMillis(settings.getTimeoutInMinutes()) < System.currentTimeMillis() - lastNotificationTimeForArea &&
                geoEventMatchesTransition(settings, geofenceTransition) &&
                !area.isExpired();
    }

    private boolean checkIsAreaInDeliveryWindow(Area.DeliveryTime deliveryTime) {
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
            Log.e(MobileMessaging.TAG, e.getMessage(), e);
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
            Log.e(MobileMessaging.TAG, e.getMessage(), e);
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

    private GeoEvent getNotificationSettingsForTransition(Area area, int geofenceTransition) {
        if (area.getEvents() == null || area.getEvents().isEmpty()) {
            return DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER;
        }

        for (GeoEvent e : area.getEvents()) {
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

    private void notifyAboutTransition(Context context, GeofenceAreas geofenceAreas, int geofenceTransition, Message message) {
        NotificationHandler.displayNotification(context, message, random.nextInt());
        sendGeoEventBroadcast(context, geofenceTransition, geofenceAreas, message);
    }

    private void sendGeoEventBroadcast(Context context, int geofenceTransition, GeofenceAreas geofenceAreas, Message message) {
        Event event = transitionEvents.get(geofenceTransition);
        if (event == null) {
            return;
        }

        Intent geofenceIntent = new Intent(event.getKey());
        geofenceIntent.putExtra(BroadcastParameter.EXTRA_GEOFENCE_AREAS, geofenceAreas);
        geofenceIntent.putExtras(message.getBundle());
        LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceIntent);
        context.sendBroadcast(geofenceIntent);
    }

    private void logGeofences(List<Area> areas, int transition) {
        for (Area a : areas) {
            Log.i(TAG, transitionNames.get(transition) + " (" + a.getTitle() + ") LAT:" + a.getLatitude() + " LON:" + a.getLongitude() + " RAD:" + a.getRadius());
        }
    }

    private Map<Message, List<Area>> findMessagesAndAreasForTriggeringGeofences(Context context, List<Geofence> geofences) {
        Map<Message, List<Area>> messagesAndAreas = new HashMap<>();
        for (Message message : messageStore.findAll(context)) {
            List<Area> geoAreas = message.getGeofenceAreasList();
            if (geoAreas == null || geoAreas.isEmpty()) {
                continue;
            }

            List<Area> areas = new ArrayList<>();
            for (Area area : geoAreas) {
                for (Geofence geofence : geofences) {
                    if (geofence.getRequestId().equalsIgnoreCase(area.getId())) {
                        areas.add(area);
                    }
                }
            }

            if (!areas.isEmpty()) {
                messagesAndAreas.put(message, areas);
            }
        }

        return messagesAndAreas;
    }
}