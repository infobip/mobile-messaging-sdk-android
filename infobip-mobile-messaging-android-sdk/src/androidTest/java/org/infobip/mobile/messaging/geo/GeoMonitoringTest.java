package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import com.google.android.gms.location.Geofence;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author sslavin
 * @since 12/02/2017.
 */

public class GeoMonitoringTest extends InstrumentationTestCase {

    private Context context;
    private MessageStore geoStore;
    private Long now;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext().getApplicationContext();
        now = System.currentTimeMillis();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        Geofencing.getInstance(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
    }

    public void test_shouldCalculateRefreshDatesForGeoStartAndExpired() throws Exception {
        // Given
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        Long millis30MinAfterNow = now + 30 * 60 * 1000;
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));
        String date30MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinAfterNow));

        saveGeoMessageToDb(date15MinAfterNow, date30MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNotNull(geofencesAndNextRefreshDate.getRight());

        Date refreshStartDate = geofencesAndNextRefreshDate.getRight().getLeft();
        Date refreshExpiryDate = geofencesAndNextRefreshDate.getRight().getRight();
        assertEquals(millis15MinAfterNow, refreshStartDate.getTime(), 3000);
        assertEquals(millis30MinAfterNow, refreshExpiryDate.getTime(), 3000);
    }

    public void test_shouldNotCalculateRefreshDateForGeoStartIfGeoExpired() throws Exception {
        // Given
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));

        saveGeoMessageToDb(date30MinBeforeNow, date15MinBeforeNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
    }

    public void test_shouldCalculateRefreshDateForGeoExpiredIfGeoExpired() throws Exception {
        // Given
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));

        saveGeoMessageToDb(date30MinBeforeNow, date15MinBeforeNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
        assertEquals(now, geofencesAndNextRefreshDate.getRight().getRight().getTime(), 3000);
    }

    public void test_shouldNotCalculateRefreshDateForGeoStartIfGeoIsMonitoredNow() throws Exception {
        // Given
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));

        saveGeoMessageToDb(date15MinBeforeNow, date15MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertFalse(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
    }

    public void test_shouldCalculateRefreshDateForGeoExpiredIfGeoIsMonitoredNow() throws Exception {
        // Given
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));

        saveGeoMessageToDb(date15MinBeforeNow, date15MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = Geofencing.calculateGeofencesToMonitorAndNextCheckDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertFalse(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
        assertEquals(millis15MinAfterNow, geofencesAndNextRefreshDate.getRight().getRight().getTime(), 3000);
    }

    private void saveGeoMessageToDb(String startTimeMillis, String expiryTimeMillis) throws JSONException {
        Geo geo = new Geo(0.0, 0.0, new ArrayList<Area>() {{
            add(new Area("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        }}, null, new ArrayList<GeoEvent>(), expiryTimeMillis, startTimeMillis, "SomeCampaignId");

        JSONObject internalData = new JSONObject(new JsonSerializer().serialize(geo));
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
                internalData,
                null,
                geo,
                "SomeDestination",
                Message.Status.UNKNOWN,
                "SomeStatusMessage"
        );
        geoStore.save(context, message);
    }

}
