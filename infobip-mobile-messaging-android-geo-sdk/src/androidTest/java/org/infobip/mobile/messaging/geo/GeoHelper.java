package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.geo.transition.GeoTransition;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

class GeoHelper {

    /**
     * Creates new geofencing transition object
     *
     * @param areaIds ids of areas to put into transition
     * @return new transition object
     */
    static GeoTransition createTransition(String... areaIds) {
        return new GeoTransition(GeoEventType.entry, new HashSet<>(Arrays.asList(areaIds)), new GeoLatLng(1.0, 1.0));
    }

    /**
     * Creates new geofencing transition object
     *
     * @param lat     latitude of transition event
     * @param lon     longitude of transition event
     * @param areaIds ids of areas to put into transition
     * @return new transition object
     */
    static GeoTransition createTransition(Double lat, Double lon, String... areaIds) {
        return new GeoTransition(GeoEventType.entry, new HashSet<>(Arrays.asList(areaIds)), new GeoLatLng(lat, lon));
    }
}