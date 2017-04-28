package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;

public enum GeoEvent {

    /**
     * It is triggered when monitored geofence area is entered.
     * <p>
     * Contains the {@link Geo} object which contains the list of all triggered geofence areas and {@link Message}.
     * <pre>
     * {@code
     * Geo geo = Geo.createFrom(intent.getExtras());
     * Message message = Message.createFrom(intent.getExtras());
     * }
     * </pre>
     */
    GEOFENCE_AREA_ENTERED("org.infobip.mobile.messaging.geo.GEOFENCE_AREA_ENTERED"),

    /**
     * It is triggered when geofence events are reported to the server.
     * <pre>
     * {@code
     * List<GeoReport> geoReports = GeoReport.createFrom(intent.getExtras());
     * }
     * </pre>
     */
    GEOFENCE_EVENTS_REPORTED("org.infobip.mobile.messaging.geo.GEOFENCE_EVENTS_REPORTED");

    private final String key;

    GeoEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
