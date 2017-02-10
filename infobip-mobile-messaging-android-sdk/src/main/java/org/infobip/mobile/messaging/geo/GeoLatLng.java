package org.infobip.mobile.messaging.geo;

/**
 * @author sslavin
 * @since 08/02/2017.
 */

public class GeoLatLng {
    private Double lat;
    private Double lng;

    public GeoLatLng(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    Double getLat() {
        return lat;
    }

    Double getLng() {
        return lng;
    }
}
