package org.infobip.mobile.messaging.geo;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import static org.junit.Assert.assertNotEquals;

/**
 * @author mstipanov
 * @since 30.03.2016.
 */
public class FCMMessageTest extends TestCase {

    public void test_geofence_expiryTime() throws Exception {
        String geofence =
        "{" +
            "\"expiryTime\":\"2016-08-06T12:20:16+03:00\"" +
        "}";

        GeoEventsTest.GeoTest geo = new JsonSerializer().deserialize(geofence, GeoEventsTest.GeoTest.class);

        assertNotNull(geo.getExpiryDate());
        assertNotEquals(0L, geo.getExpiryDate().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-12-06T13:20:16+0300\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoEventsTest.GeoTest.class);

        assertNotNull(geo.getExpiryDate());
        assertNotEquals(0L, geo.getExpiryDate().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-08-31T14:20:16+03\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoEventsTest.GeoTest.class);

        assertNotNull(geo.getExpiryDate());
        assertNotEquals(0L, geo.getExpiryDate().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-08-31T14:20:16Z\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoEventsTest.GeoTest.class);

        assertNotNull(geo.getExpiryDate());
        assertNotEquals(0L, geo.getExpiryDate().getTime());
    }
}
