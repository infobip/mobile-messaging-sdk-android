package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

/**
 * @author sslavin
 * @since 06/02/2017.
 */

class GeoNotificationHelper {

    private static final String AREA_NOTIFIED_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.notified.";
    private static final String AREA_LAST_TIME_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.last.time.";
    private static final GeoEventSettings DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER = new GeoEventSettings(GeoEventType.entry, 1, 0L);

    private Context context;
    private Broadcaster broadcaster;

    public GeoNotificationHelper(Context context, Broadcaster broadcaster) {
        this.context = context;
        this.broadcaster = broadcaster;
    }

    /**
     * Broadcasts geofencing events and displays appropriate notifications for geo events
     * @param messages messages with geo to notify
     */
    void notifyAboutGeoTransitions(Map<Message, GeoEventType> messages) {
        for (Message m : messages.keySet()) {
            GeoEventType eventType = messages.get(m);

            setLastNotificationTimeForArea(context, m.getGeo().getCampaignId(), eventType, Time.now());
            setNumberOfDisplayedNotificationsForArea(context, m.getGeo().getCampaignId(), eventType,
                        getNumberOfDisplayedNotificationsForArea(context, m.getGeo().getCampaignId(), eventType) + 1);

            notifyAboutTransition(m.getGeo(), m, eventType);
        }
    }

    /**
     * Determines if transition should be reported to server
     * @param originalMessage original push signaling message with areas
     * @param event transition type
     * @return returns true if transition could be reported now
     */
    static boolean shouldReportTransition(Context context, Message originalMessage, GeoEventType event) {
        int numberOfDisplayedNotifications = getNumberOfDisplayedNotificationsForArea(context, originalMessage.getGeo().getCampaignId(), event);
        long lastNotificationTimeForArea = getLastNotificationTimeForArea(context, originalMessage.getGeo().getCampaignId(), event);
        Geo geo = originalMessage.getGeo();
        GeoEventSettings settings = getNotificationSettingsForTransition(geo.getEvents(), event);

        boolean isInDeliveryWindow = checkIsAreaInDeliveryWindow(geo.getDeliveryTime());

        return settings != null &&
                isInDeliveryWindow &&
                (settings.getLimit() > numberOfDisplayedNotifications || settings.getLimit() == GeoEventSettings.UNLIMITED_RECURRING) &&
                TimeUnit.MINUTES.toMillis(settings.getTimeoutInMinutes()) < Time.now() - lastNotificationTimeForArea &&
                geoEventMatchesTransition(settings, event) &&
                !geo.isExpired();
    }

    private static boolean checkIsAreaInDeliveryWindow(DeliveryTime deliveryTime) {
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

    private static boolean shouldDeliverToday(String daysPayload) {
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

    private static boolean checkIsDeliveryInTimeInterval(String timeInterval) throws ParseException {
        if (timeInterval == null) {
            return false;
        }

        String[] timeIntervalStartEnd = timeInterval.split("/");
        String startTime = timeIntervalStartEnd[0];
        String endTime = timeIntervalStartEnd[1];

        return DateTimeUtil.isCurrentTimeBetweenDates(startTime, endTime);
    }

    private static GeoEventSettings getNotificationSettingsForTransition(List<GeoEventSettings> eventFilters, GeoEventType event) {
        if (eventFilters == null || eventFilters.isEmpty()) {
            return DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER;
        }

        for (GeoEventSettings e : eventFilters) {
            if (!geoEventMatchesTransition(e, event)) {
                continue;
            }
            return e;
        }
        return null;
    }

    private static String areaNotificationNumKey(String campaignId, GeoEventType event) {
        return AREA_NOTIFIED_PREF_PREFIX + campaignId + "-" + event.ordinal();
    }

    private static String areaNotificationTimeKey(String campaignId, GeoEventType event) {
        return AREA_LAST_TIME_PREF_PREFIX + campaignId + "-" + event.ordinal();
    }

    private static boolean geoEventMatchesTransition(GeoEventSettings eventSetting, GeoEventType eventType) {
        return eventSetting.getType().equals(DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER.getType()) && eventType == GeoEventType.entry;
    }

    private static int getNumberOfDisplayedNotificationsForArea(Context context, String campaignId, GeoEventType event) {
        return PreferenceHelper.findInt(context, areaNotificationNumKey(campaignId, event), 0);
    }

    private static void setNumberOfDisplayedNotificationsForArea(Context context, String campaignId, GeoEventType event, int n) {
        PreferenceHelper.saveInt(context, areaNotificationNumKey(campaignId, event), n);
    }

    private static long getLastNotificationTimeForArea(Context context, String campaignId, GeoEventType event) {
        return PreferenceHelper.findLong(context, areaNotificationTimeKey(campaignId, event), 0);
    }

    private static void setLastNotificationTimeForArea(Context context, String campaignId, GeoEventType event, long timeMs) {
        PreferenceHelper.saveLong(context, areaNotificationTimeKey(campaignId, event), timeMs);
    }

    private void notifyAboutTransition(Geo geo, Message message, GeoEventType event) {
        NotificationHandler.displayNotification(context, message);

        broadcaster.messageReceived(message);
        broadcaster.geoEvent(event, message, geo);
    }
}
