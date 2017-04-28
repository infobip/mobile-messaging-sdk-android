package org.infobip.mobile.messaging.geo.transition;

import android.content.Intent;
import android.location.Location;
import android.support.v4.util.ArraySet;
import android.util.SparseArray;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoLatLng;

import java.util.Set;

/**
 * @author sslavin
 * @since 08/02/2017.
 */

class GeoTransitionHelper {

    static class GeofenceNotAvailableException extends RuntimeException {
    }

    /**
     * Supported geofence transition events
     */
    private static SparseArray<GeoEventType> supportedTransitionEvents = new SparseArray<GeoEventType>() {{
        put(Geofence.GEOFENCE_TRANSITION_ENTER, GeoEventType.entry);
    }};

    /**
     * Resolves transition information from geofencing intent
     *
     * @param intent geofencing intent
     * @return transition information
     * @throws RuntimeException if information cannot be resolved
     */
    static GeoTransition resolveTransitionFromIntent(Intent intent) throws RuntimeException {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            throw new RuntimeException("Geofencing event is null, cannot process");
        }

        if (geofencingEvent.hasError()) {
            if (geofencingEvent.getErrorCode() == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                throw new GeofenceNotAvailableException();
            }
            throw new RuntimeException("ERROR: " + GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode()));
        }

        GeoEventType event = supportedTransitionEvents.get(geofencingEvent.getGeofenceTransition());
        if (event == null) {
            throw new RuntimeException("Transition is not supported: " + geofencingEvent.getGeofenceTransition());
        }

        Set<String> triggeringRequestIds = new ArraySet<>();
        for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
            triggeringRequestIds.add(geofence.getRequestId());
        }

        Location location = geofencingEvent.getTriggeringLocation();
        return new GeoTransition(event, triggeringRequestIds, new GeoLatLng(location.getLatitude(), location.getLongitude()));
    }
}
