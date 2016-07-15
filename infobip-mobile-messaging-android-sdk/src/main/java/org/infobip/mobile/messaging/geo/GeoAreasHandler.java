package org.infobip.mobile.messaging.geo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Geo;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * @author pandric
 * @since 24.06.2016.
 */
class GeoAreasHandler {

    protected static final String TAG = "GeofenceTransitions";

    void handleNotification(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) return;

        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            List<Geo.Area> areasList = getAreasList(context, triggeringGeofences);
            displayNotifications(context, areasList);

            if (!areasList.isEmpty()) {
                Geo geo = new Geo(areasList);
                Intent geofenceAreaEntered = new Intent(Event.GEOFENCE_AREA_ENTERED.getKey());
                intent.putExtra(BroadcastParameter.EXTRA_GEOFENCE_AREAS, geo);
                LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceAreaEntered);
            }

        } else {
            Log.e(TAG, "Geofence transition - invalid type");
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

    private void displayNotifications(Context context, List<Geo.Area> triggeredGeoAreasList) {
        if (triggeredGeoAreasList == null || triggeredGeoAreasList.isEmpty()) return;

        for (Geo.Area area : triggeredGeoAreasList) {
            displayNotificationForArea(context, area);
        }
    }

    private List<Geo.Area> getAreasList(Context context, List<Geofence> triggeringGeofences) {
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().bind(context);
        List<Geo.Area> areasList = new ArrayList<>(triggeringGeofences.size());

        for (Message message : messages) {
            List<Geo.Area> geoAreasList = message.getGeoAreasList();
            for (Geo.Area area : geoAreasList) {
                for (Geofence geofence : triggeringGeofences) {
                    if (geofence.getRequestId().equalsIgnoreCase(area.getId())) {
                        areasList.add(area);
                    }
                }
            }
        }

        return areasList;
    }

    @SuppressWarnings("ResourceType")
    private void displayNotificationForArea(Context context, Geo.Area area) {
        if (area == null) return;
        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();

        if (null == notificationSettings) {
            return;
        }

        if (!notificationSettings.isDisplayNotificationEnabled() || null == notificationSettings.getCallbackActivity()) {
            return;
        }

        Intent intent = new Intent(context, notificationSettings.getCallbackActivity());
        intent.addFlags(notificationSettings.getIntentFlags());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, notificationSettings.getPendingIntentFlags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setColor(Color.RED)
                .setDefaults(notificationSettings.getNotificationDefaults())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentTitle(String.format(Locale.getDefault(), "%s", area.getTitle()))
                .setContentText(String.format(Locale.getDefault(), "Radius: %d m", area.getRadius()));

        int icon = notificationSettings.getDefaultIcon();
        builder.setSmallIcon(icon);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification build = builder.build();
        notificationManager.notify(new Random().nextInt(), build);
    }
}
