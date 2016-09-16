package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.GeofenceAreas;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author pandric
 * @since 24.06.2016.
 */
class GeoAreasHandler {

    protected static final String TAG = "GeofenceTransitions";

    MessageStore messageStore = new SharedPreferencesMessageStore();

    void handleTransition(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) return;

        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Tuple<List<Message>, List<GeofenceAreas.Area>> messagesAndAreas =
                getMessagesAndAreasForTriggeringGeofences(context, geofencingEvent.getTriggeringGeofences());

        LogGeofenceTransition(geofenceTransition);
        LogGeofences(messagesAndAreas.getRight());

        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return;
        }

        Random random = new Random();
        for (Message message : messagesAndAreas.getLeft()) {
            NotificationHandler.displayNotification(context, message, random.nextInt());
        }

        Location triggeringLocation = geofencingEvent.getTriggeringLocation();
        GeofenceAreas geofenceAreas = new GeofenceAreas(triggeringLocation.getLatitude(),
                triggeringLocation.getLongitude(), messagesAndAreas.getRight());

        Intent geofenceIntent = new Intent(Event.GEOFENCE_AREA_ENTERED.getKey());
        geofenceIntent.putExtra(BroadcastParameter.EXTRA_GEOFENCE_AREAS, geofenceAreas);
        LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceIntent);
        context.sendBroadcast(geofenceIntent);
    }

    private void LogGeofenceTransition(int t) {
        switch (t) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Log.i(TAG, "GEOFENCE_TRANSITION_ENTER");
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                Log.i(TAG, "GEOFENCE_TRANSITION_DWELL");
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Log.i(TAG, "GEOFENCE_TRANSITION_EXIT");
                break;
            default:
                Log.i(TAG, "Transition type is invalid: " + t);
        }
    }

    private void LogGeofences(List<GeofenceAreas.Area> areas) {
        for (GeofenceAreas.Area a : areas) {
            Log.i(TAG, "GEOFENCE (" + a.getTitle() + ") LAT:" + a.getLatitude() + " LON:" + a.getLongitude() + " RAD:" + a.getRadius());
        }
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown geofence error";
        }
    }

    private Tuple<List<Message>, List<GeofenceAreas.Area>> getMessagesAndAreasForTriggeringGeofences(Context context, List<Geofence> geofences) {
        List<Message> messages = new ArrayList<>();
        Map<String, GeofenceAreas.Area> areas = new HashMap<>();

        for (Message message : messageStore.findAll(context)) {
            List<GeofenceAreas.Area> geoAreas = message.getGeofenceAreasList();
            if (geoAreas == null || geoAreas.isEmpty()) {
                continue;
            }

            boolean isMessageTriggered = false;
            for (GeofenceAreas.Area area : geoAreas) {
                for (Geofence geofence : geofences) {
                    if (geofence.getRequestId().equalsIgnoreCase(area.getId())) {
                        isMessageTriggered = true;
                        areas.put(area.getId(), area);
                    }
                }
            }

            if (isMessageTriggered) {
                messages.add(message);
            }
        }

        List<GeofenceAreas.Area> areaList = new ArrayList<>(areas.values());
        return new Tuple<>(messages, areaList);
    }
}
