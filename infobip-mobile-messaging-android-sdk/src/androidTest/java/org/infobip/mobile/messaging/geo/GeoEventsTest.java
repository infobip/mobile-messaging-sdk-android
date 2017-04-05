package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

/**
 * Created by pandric on 20/09/16.
 */
public class GeoEventsTest extends MobileMessagingTestCase {

    private long timeDelta;

    private class GeoTest extends Geo {
        public GeoTest() {
            super(null, null, null, null, null, null, null, null);
        }

        List<GeoEventSettings> getEventFilters() {
            return getEvents();
        }
    }

    @Test
    public void test_handle_event_notification_settings() throws InterruptedException {
        GeoTest geo = new JsonSerializer().deserialize(internalData(), GeoTest.class);

        int entryCnt = 0;
        for (int i = 0; i < 5; i++) {

            List<GeoEventSettings> events = geo.getEventFilters();
            for (GeoEventSettings event : events) {

                for (Area area : geo.getAreasList()) {

                    // time travel - simulate delay between events
                    advanceTimeByMS(TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes() + 1));

                    long lastDisplayTime = getLastNotificationTimeForArea(area);
                    int timesTriggered = getNumberOfDisplayedNotificationsForArea(area);

                    long timeIntervalBetweenEvents = currentTimeMillis() - lastDisplayTime;
                    boolean isTimeoutExpired = timeIntervalBetweenEvents > TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes());

                    int eventLimit = event.getLimit();
                    boolean isLimitBreached = eventLimit != GeoEventSettings.UNLIMITED_RECURRING && timesTriggered >= eventLimit;

                    if (isTimeoutExpired && !isLimitBreached) {

                        if (eventLimit != GeoEventSettings.UNLIMITED_RECURRING) {
                            ++timesTriggered;
                        }

                        lastDisplayTime = currentTimeMillis();
                        setLastNotificationTimeForArea(area, lastDisplayTime);
                        setNumberOfDisplayedNotificationsForArea(area, timesTriggered);

                        entryCnt++;
                    }
                }
            }
        }

        assertEquals(entryCnt, 5);
    }

    private int getNumberOfDisplayedNotificationsForArea(Area area) {
        return PreferenceHelper.findInt(context, "num" + area.getId(), 0);
    }

    private long getLastNotificationTimeForArea(Area area) {
        return PreferenceHelper.findLong(context, "time" + area.getId(), 0);
    }

    private void setNumberOfDisplayedNotificationsForArea(Area area, int n) {
        PreferenceHelper.saveInt(context, "num" + area.getId(), n);
    }

    private void setLastNotificationTimeForArea(Area area, long timeMs) {
        PreferenceHelper.saveLong(context, "time" + area.getId(), timeMs);
    }

    private void advanceTimeByMS(long milliseconds) {
        timeDelta += milliseconds;
    }

    private long currentTimeMillis() {
        return Time.now() + timeDelta;
    }

    private String internalData() {
        return "{\n" +
                " \"event\": [\n" +
                "  {\n" +
                "    \"type\": \"entry\",\n" +
                "    \"limit\": 0,\n" +
                "    \"timeoutInMinutes\": 1\n" +
                "  }],\n" +
                "  \"geo\": [\n" +
                "    {\n" +
                "      \"radiusInMeters\": 1000,\n" +
                "      \"latitude\": 45.81285,\n" +
                "      \"id\": \"6713245DA3638FDECFE448C550AD7681\",\n" +
                "      \"expiry\": 1469292326099,\n" +
                "      \"title\": \"Zagreb\",\n" +
                "      \"favorite\": false,\n" +
                "      \"longitude\": 15.97749\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
