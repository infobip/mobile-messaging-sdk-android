package org.infobip.mobile.messaging.geo.transition;


import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoLatLng;

import java.util.Set;

/**
 * @author sslavin
 * @since 08/02/2017.
 * <p>
 * Class that contains transition information
 */
public class GeoTransition {
    @NonNull
    private final GeoEventType eventType;

    @NonNull
    private final Set<String> requestIds;

    @NonNull
    private final GeoLatLng triggeringLocation;

    public GeoTransition(@NonNull GeoEventType eventType, @NonNull Set<String> requestIds, @NonNull GeoLatLng triggeringLocation) {
        this.eventType = eventType;
        this.requestIds = requestIds;
        this.triggeringLocation = triggeringLocation;
    }

    @NonNull
    public GeoEventType getEventType() {
        return eventType;
    }

    @NonNull
    public Set<String> getRequestIds() {
        return requestIds;
    }

    @NonNull
    public GeoLatLng getTriggeringLocation() {
        return triggeringLocation;
    }
}