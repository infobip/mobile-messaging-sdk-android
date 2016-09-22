package org.infobip.mobile.messaging;

import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.GeofenceAreas.Area.GeoEvent;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by pandric on 20/09/16.
 */
public class GeoEventsTest extends InstrumentationTestCase {

    private Context context;

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

    private int getNumberOfDisplayedNotificationsForArea(GeofenceAreas.Area area) {
        return PreferenceHelper.findInt(context, "num" + area.getId(), 0);
    }

    private long getLastNotificationTimeForArea(GeofenceAreas.Area area) {
        return PreferenceHelper.findLong(context, "time" + area.getId(), 0);
    }

    private void setNumberOfDisplayedNotificationsForArea(GeofenceAreas.Area area, int n) {
        PreferenceHelper.saveInt(context, "num" + area.getId(), n);
    }

    private void setLastNotificationTimeForArea(GeofenceAreas.Area area, long timeMs) {
        PreferenceHelper.saveLong(context, "time" + area.getId(), timeMs);
    }

    public void test_handle_geo_events() throws InterruptedException {
        GeofenceAreas geofenceAreas = new JsonSerializer().deserialize(internalData(), GeofenceAreas.class);
        List<GeofenceAreas.Area> geofenceAreasList = geofenceAreas.getAreasList();

        int entryCnt = 0;

        for (int i = 0; i < 5; i++) {

            for (GeofenceAreas.Area area : geofenceAreasList) {

                List<GeoEvent> events = area.getEvents();
                for (GeoEvent event : events) {

                    Thread.sleep(TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes() + 1)); // simulate delay between events

                    long lastDisplayTime = getLastNotificationTimeForArea(area);
                    int timesTriggered = getNumberOfDisplayedNotificationsForArea(area);

                    long timeIntervalBetweenEvents = System.currentTimeMillis() - lastDisplayTime;
                    boolean isTimeoutExpired = timeIntervalBetweenEvents > TimeUnit.MINUTES.toMillis(event.getTimeoutInMinutes());

                    int eventLimit = event.getLimit();
                    boolean isLimitBreached = eventLimit != GeoEvent.UNLIMITED_RECURRING && timesTriggered >= eventLimit;

                    if (isTimeoutExpired && !isLimitBreached) {

                        if (eventLimit != GeoEvent.UNLIMITED_RECURRING) {
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
                "  \"geo\": [\n" +
                "    {\n" +
                "      \"radiusInMeters\": 1000,\n" +
                "      \"latitude\": 45.81285,\n" +
                "      \"id\": \"6713245DA3638FDECFE448C550AD7681\",\n" +
                "      \"expiry\": 1469292326099,\n" +
                "      \"title\": \"Zagreb\",\n" +
                "      \"favorite\": false,\n" +
                "      \"longitude\": 15.97749,\n" +
                "      \"event\": [\n" +
                "        {\n" +
                "          \"type\": \"entry\",\n" +
                "          \"limit\": 0,\n" +
                "          \"timeout\": 1\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"exit\",\n" +
                "          \"limit\": 2,\n" +
                "          \"timeout\": 2\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"remain\",\n" +
                "          \"limit\": 3,\n" +
                "          \"timeout\": 3\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
