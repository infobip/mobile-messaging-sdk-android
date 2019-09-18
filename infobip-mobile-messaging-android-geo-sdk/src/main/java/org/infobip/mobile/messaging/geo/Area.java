package org.infobip.mobile.messaging.geo;

import com.google.android.gms.location.Geofence;

import org.infobip.mobile.messaging.platform.Time;

import java.util.Date;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class Area {

    private final String id;
    private final String title;
    private final Double latitude;
    private final Double longitude;
    private final Integer radiusInMeters;

    public Area(String id, String title, Double latitude, Double longitude, Integer radiusInMeters) {
        this.id = id;
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusInMeters = radiusInMeters;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getRadius() {
        return radiusInMeters;
    }

    public Geofence toGeofence(Date expiryDate) {
        long expirationDurationMillis = 0L;
        if (expiryDate != null) {
            expirationDurationMillis = expiryDate.getTime() - Time.now();
        }
        if (expirationDurationMillis <= 0) {
            expirationDurationMillis = Geofence.NEVER_EXPIRE;
        }

        return new Geofence.Builder()
                .setCircularRegion(getLatitude(), getLongitude(), getRadius())
                .setRequestId(getId())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(expirationDurationMillis)
                .build();
    }

    /**
     * Geofence is valid and it can be monitored
     *
     * @return true if area is valid
     */
    public boolean isValid() {
        return getId() != null &&
                getLatitude() != null &&
                getLongitude() != null &&
                getRadius() != null;
    }
}
