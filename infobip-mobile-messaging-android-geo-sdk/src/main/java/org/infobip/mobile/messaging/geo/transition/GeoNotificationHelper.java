package org.infobip.mobile.messaging.geo.transition;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.geo.DeliveryTime;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventSettings;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoMessage;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.platform.GeoBroadcaster;
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

public class GeoNotificationHelper {

    private static final String AREA_NOTIFIED_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.notified.";
    private static final String AREA_LAST_TIME_PREF_PREFIX = "org.infobip.mobile.messaging.geo.area.last.time.";
    private static final GeoEventSettings DEFAULT_NOTIFICATION_SETTINGS_FOR_ENTER = new GeoEventSettings(GeoEventType.entry, 1, 0L);

    private final Context context;
    private final GeoBroadcaster geoBroadcaster;
    private final Broadcaster messageBroadcaster;
    private final NotificationHandler notificationHandler;

    public GeoNotificationHelper(Context context, GeoBroadcaster geoBroadcaster, Broadcaster messageBroadcaster, NotificationHandler notificationHandler) {
        this.context = context;
        this.geoBroadcaster = geoBroadcaster;
        this.messageBroadcaster = messageBroadcaster;
        this.notificationHandler = notificationHandler;
    }

    /**
     * Broadcasts geofencing events and displays appropriate notifications for geo events
     *
     * @param messages messages with geo to notify
     */
    public void notifyAboutGeoTransitions(Map<Message, GeoEventType> messages) {
        for (Message m : messages.keySet()) {
            GeoEventType eventType = messages.get(m);

            Geo geo = GeoDataMapper.geoFromInternalData(m.getInternalData());
            if (geo == null) continue;

            setLastNotificationTimeForArea(context, geo.getCampaignId(), eventType, Time.now());
            setNumberOfDisplayedNotificationsForArea(context, geo.getCampaignId(), eventType,
                    getNumberOfDisplayedNotificationsForArea(context, geo.getCampaignId(), eventType) + 1);

            notifyAboutTransition(geo, m, eventType);
        }
    }

    /**
     * Determines if transition should be reported to server
     *
     * @param geo   from original push signaling message with areas
     * @param event transition type
     * @return returns true if transition could be reported now
     */
    public static boolean shouldReportTransition(Context context, Geo geo, GeoEventType event) {
        int numberOfDisplayedNotifications = getNumberOfDisplayedNotificationsForArea(context, geo.getCampaignId(), event);
        long lastNotificationTimeForArea = getLastNotificationTimeForArea(context, geo.getCampaignId(), event);
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
        notificationHandler.displayNotification(message);

        messageBroadcaster.messageReceived(message);
        geoBroadcaster.geoEvent(event, GeoMessage.createFrom(message, geo));
    }
}
