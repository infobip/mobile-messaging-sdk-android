package org.infobip.mobile.messaging.geo;

import com.google.android.gms.location.Geofence;

import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.geo.geofencing.Geofencing;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingImpl;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 12/02/2017.
 */

public class GeofencingTest extends MobileMessagingTestCase {

    private Long now;
    private Geofencing geofencingMock;
    private MobileGeoImpl mobileGeoImpl;
    private GeofencingImpl geofencingImpl;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        now = Time.now();

        enableMessageStoreForReceivedMessages();
        mobileGeoImpl = MobileGeoImpl.getInstance(context);
        geofencingMock = Mockito.mock(Geofencing.class);
        geofencingImpl = GeofencingImpl.getInstance(context);
    }

    @Test
    @SuppressWarnings("MissingPermission")
    public void shouldActivateGeo() {
        // When
        mobileGeoImpl.activateGeofencing(geofencingMock);

        // Then
        Mockito.verify(geofencingMock, Mockito.times(1)).setGeoComponentsEnabledSettings(context, true);
        Mockito.verify(geofencingMock, Mockito.times(1)).startGeoMonitoring();
        assertTrue(GeofencingHelper.isActivated(context));
    }

    @Test
    @SuppressWarnings("MissingPermission")
    public void shouldNotActivateGeo() {
        // When
        mobileGeoImpl.activateGeofencing(null);  //null if withGeofencing or activateGeofencing aren't called

        // Then
        Mockito.verify(geofencingMock, Mockito.never()).setGeoComponentsEnabledSettings(context, true);
        Mockito.verify(geofencingMock, Mockito.never()).startGeoMonitoring();
        assertFalse(mobileGeoImpl.isGeofencingActivated());
    }

    @Test
    public void shouldDeactivateGeo() {
        // When
        mobileGeoImpl.deactivateGeofencing(geofencingMock);

        // Then
        Mockito.verify(geofencingMock, Mockito.times(1)).setGeoComponentsEnabledSettings(context, false);
        Mockito.verify(geofencingMock, Mockito.times(1)).stopGeoMonitoring();
        assertFalse(GeofencingHelper.isActivated(context));
    }

    @Test
    public void shouldCalculateRefreshDatesForGeoStartAndExpired() throws Exception {
        // Given
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        Long millis30MinAfterNow = now + 30 * 60 * 1000;
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));
        String date30MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinAfterNow));

        saveGeoMessageToDb(date15MinAfterNow, date30MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = geofencingImpl.calculateGeofencesToMonitorDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNotNull(geofencesAndNextRefreshDate.getRight());

        Date refreshStartDate = geofencesAndNextRefreshDate.getRight().getLeft();
        Date refreshExpiryDate = geofencesAndNextRefreshDate.getRight().getRight();
        assertEquals(millis15MinAfterNow, refreshStartDate.getTime(), 3000);
        assertEquals(millis30MinAfterNow, refreshExpiryDate.getTime(), 3000);
    }

    @Test
    public void shouldNotCalculateRefreshDateForGeoStartIfGeoExpired() throws Exception {
        // Given
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));

        saveGeoMessageToDb(date30MinBeforeNow, date15MinBeforeNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = geofencingImpl.calculateGeofencesToMonitorDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
    }

    @Test
    public void shouldCalculateRefreshDateForGeoExpiredIfGeoExpired() throws Exception {
        // Given
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));

        saveGeoMessageToDb(date30MinBeforeNow, date15MinBeforeNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = geofencingImpl.calculateGeofencesToMonitorDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertTrue(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
        assertEquals(now, geofencesAndNextRefreshDate.getRight().getRight().getTime(), 3000);
    }

    @Test
    public void shouldNotCalculateRefreshDateForGeoStartIfGeoIsMonitoredNow() throws Exception {
        // Given
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));

        saveGeoMessageToDb(date15MinBeforeNow, date15MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = geofencingImpl.calculateGeofencesToMonitorDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertFalse(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
    }

    @Test
    public void shouldCalculateRefreshDateForGeoExpiredIfGeoIsMonitoredNow() throws Exception {
        // Given
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));

        saveGeoMessageToDb(date15MinBeforeNow, date15MinAfterNow);

        // When
        Tuple<List<Geofence>, Tuple<Date, Date>> geofencesAndNextRefreshDate = geofencingImpl.calculateGeofencesToMonitorDates(geoStore);

        // Then
        assertNotNull(geofencesAndNextRefreshDate);
        assertFalse(geofencesAndNextRefreshDate.getLeft().isEmpty());
        assertNull(geofencesAndNextRefreshDate.getRight().getLeft());
        assertEquals(millis15MinAfterNow, geofencesAndNextRefreshDate.getRight().getRight().getTime(), 3000);
    }

    private void saveGeoMessageToDb(String startTimeMillis, String expiryTimeMillis) {
        Geo geo = createGeo(0.0, 0.0, expiryTimeMillis, startTimeMillis, "SomeCampaignId", null, createArea("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        createMessage(context, "SomeMessageId", true, geo);
    }
}
