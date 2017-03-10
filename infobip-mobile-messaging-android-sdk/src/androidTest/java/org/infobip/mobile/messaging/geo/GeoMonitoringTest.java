package org.infobip.mobile.messaging.geo;

import com.google.android.gms.location.Geofence;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author sslavin
 * @since 12/02/2017.
 */

public class GeoMonitoringTest extends InfobipAndroidTestCase {

    private MessageStore messageStore;
    private Long now;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        now = System.currentTimeMillis();

        Geofencing.getInstance(context);

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        messageStore = MobileMessaging.getInstance(context).getMessageStore();
        messageStore.deleteAll(context);
    }

    public void test_shouldCalculateRefreshDateForGeoStart() throws Exception {
        // Given
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        Long millis30MinAfterNow = now + 30 * 60 * 1000;
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));
        String date30MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinAfterNow));

        Geo geo = new Geo(0.0, 0.0, null, date30MinAfterNow, date15MinAfterNow, "SomeCampaignId", new ArrayList<Area>() {{
            add(new Area("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        }}, new ArrayList<GeoEventSettings>());

        Message message = new Message(
                "SomeMessageId",
                "SomeTitle",
                "SomeBody",
                "SomeSound",
                true,
                "SomeIcon",
                true,
                "SomeCategory",
                "SomeFrom",
                now,
                0,
                null,
                geo,
                "SomeDestination",
                Message.Status.UNKNOWN,
                "SomeStatusMessage"
        );
        messageStore.save(context, message);

        // When
        Tuple<List<Geofence>, Date> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDate(messageStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNotNull(geofencesAndNextRefreshDate.getRight());
        assertEquals(millis15MinAfterNow, geofencesAndNextRefreshDate.getRight().getTime(), 3000);
    }

    public void test_shouldNotCalculateRefreshDateIfGeoExpired() throws Exception {
        // Given
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));

        Geo geo = new Geo(0.0, 0.0, null, date15MinBeforeNow, date30MinBeforeNow, "SomeCampaignId", new ArrayList<Area>() {{
            add(new Area("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        }}, new ArrayList<GeoEventSettings>());

        Message message = new Message(
                "SomeMessageId",
                "SomeTitle",
                "SomeBody",
                "SomeSound",
                true,
                "SomeIcon",
                true,
                "SomeCategory",
                "SomeFrom",
                now,
                0,
                null,
                geo,
                "SomeDestination",
                Message.Status.UNKNOWN,
                "SomeStatusMessage"
        );
        messageStore.save(context, message);

        // When
        Tuple<List<Geofence>, Date> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDate(messageStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight());
    }

    public void test_shouldNotCalculateRefreshDateIfGeoIsMonitoredNow() throws Exception {
        // Given
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));

        Geo geo = new Geo(0.0, 0.0, null, date15MinBeforeNow, date15MinAfterNow, "SomeCampaignId", new ArrayList<Area>() {{
            add(new Area("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        }}, new ArrayList<GeoEventSettings>());

        Message message = new Message(
                "SomeMessageId",
                "SomeTitle",
                "SomeBody",
                "SomeSound",
                true,
                "SomeIcon",
                true,
                "SomeCategory",
                "SomeFrom",
                now,
                0,
                null,
                geo,
                "SomeDestination",
                Message.Status.UNKNOWN,
                "SomeStatusMessage"
        );
        messageStore.save(context, message);

        // When
        Tuple<List<Geofence>, Date> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDate(messageStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight());
    }
}
