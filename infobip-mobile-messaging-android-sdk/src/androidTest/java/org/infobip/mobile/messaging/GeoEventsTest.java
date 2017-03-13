package org.infobip.mobile.messaging;

import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEvent;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by pandric on 20/09/16.
 */
public class GeoEventsTest extends InstrumentationTestCase {

    private Context context;
    private long timeDelta;

    private class GeoTest extends Geo {
        public GeoTest() {
            super(null, null, null, null, null, null, null);
        }

        List<GeoEvent> getEventFilters() {
            return getEvents();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_handle_event_notification_settings() throws InterruptedException {
        GeoTest geo = new JsonSerializer().deserialize(internalData(), GeoTest.class);

        int entryCnt = 0;
        for (int i = 0; i < 5; i++) {

            List<GeoEvent> events = geo.getEventFilters();
            for (GeoEvent event : events) {

                for (Area area : geo.getAreasList()) {

                    // time travel - simulate delay between events
                    advanceTimeByMS(TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes() + 1));

                    long lastDisplayTime = getLastNotificationTimeForArea(area);
                    int timesTriggered = getNumberOfDisplayedNotificationsForArea(area);

                    long timeIntervalBetweenEvents = currentTimeMillis() - lastDisplayTime;
                    boolean isTimeoutExpired = timeIntervalBetweenEvents > TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes());

                    int eventLimit = event.getLimit();
                    boolean isLimitBreached = eventLimit != GeoEvent.UNLIMITED_RECURRING && timesTriggered >= eventLimit;

                    if (isTimeoutExpired && !isLimitBreached) {

                        if (eventLimit != GeoEvent.UNLIMITED_RECURRING) {
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
        return System.currentTimeMillis() + timeDelta;
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
