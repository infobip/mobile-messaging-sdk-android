package org.infobip.mobile.messaging;

import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventSetting;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by pandric on 20/09/16.
 */
public class GeoEventsTest extends InstrumentationTestCase {

    private Context context;

    private class GeoTest extends Geo {
        public GeoTest() {
            super(null, null, null, null, null, null, new ArrayList<Area>(), null);
        }

        List<GeoEventSetting> getEventFilters() {
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

    public void test_handle_geo_events() throws InterruptedException {
        GeoTest geo = new JsonSerializer().deserialize(internalData(), GeoTest.class);

        int entryCnt = 0;

        // TODO: Implement with reasonable test time, otherwise it takes forever to run
        for (int i = 0; i < 5; i++) {

            List<GeoEventSetting> events = geo.getEventFilters();
            for (GeoEventSetting event : events) {

                for (Area area : geo.getAreasList()) {

                    Thread.sleep(TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes() + 1)); // simulate delay between events

                    long lastDisplayTime = getLastNotificationTimeForArea(area);
                    int timesTriggered = getNumberOfDisplayedNotificationsForArea(area);

                    long timeIntervalBetweenEvents = System.currentTimeMillis() - lastDisplayTime;
                    boolean isTimeoutExpired = timeIntervalBetweenEvents > TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes());

                    int eventLimit = event.getLimit();
                    boolean isLimitBreached = eventLimit != GeoEventSetting.UNLIMITED_RECURRING && timesTriggered >= eventLimit;

                    if (isTimeoutExpired && !isLimitBreached) {

                        if (eventLimit != GeoEventSetting.UNLIMITED_RECURRING) {
                            ++timesTriggered;
                        }

                        lastDisplayTime = System.currentTimeMillis();
                        setLastNotificationTimeForArea(area, lastDisplayTime);
                        setNumberOfDisplayedNotificationsForArea(area, timesTriggered);

                        entryCnt++;
                    }
                }
            }
        }

        assertEquals(entryCnt, 5);
    }

    private String internalData() {
        return "{\n" +
                " \"event\": [\n" +
                "  {\n" +
                "    \"type\": \"entry\",\n" +
                "    \"limit\": 0,\n" +
                "    \"timeoutInMinutes\": 1\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"exit\",\n" +
                "    \"limit\": 2,\n" +
                "    \"timeoutInMinutes\": 2\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"remain\",\n" +
                "    \"limit\": 3,\n" +
                "    \"timeoutInMinutes\": 3\n" +
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
