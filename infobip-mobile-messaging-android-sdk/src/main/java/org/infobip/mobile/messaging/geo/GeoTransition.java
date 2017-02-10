package org.infobip.mobile.messaging.geo;

/**
 * @author sslavin
 * @since 08/02/2017.
 */

import android.support.annotation.NonNull;

import java.util.Set;

/**
 * Class that contains transition information
 */
class GeoTransition {
    @NonNull
    private GeoEventType eventType;

    @NonNull
    private Set<String> requestIds;

    @NonNull
    private GeoLatLng triggeringLocation;

    GeoTransition(@NonNull GeoEventType eventType, @NonNull Set<String> requestIds, @NonNull GeoLatLng triggeringLocation) {
        this.eventType = eventType;
        this.requestIds = requestIds;
        this.triggeringLocation = triggeringLocation;
    }

    @NonNull
    GeoEventType getEventType() {
        return eventType;
    }

    @NonNull
    Set<String> getRequestIds() {
        return requestIds;
    }

    @NonNull
    GeoLatLng getTriggeringLocation() {
        return triggeringLocation;
    }
}