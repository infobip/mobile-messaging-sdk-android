package org.infobip.mobile.messaging.geo.transition;

import android.app.IntentService;
import android.content.Intent;

import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;

public class GeofenceTransitionsIntentService extends IntentService {


    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final GeoAreasHandler geoAreasHandler = new GeoAreasHandler(this, new AndroidGeoBroadcaster(this));
        geoAreasHandler.handleTransition(intent);
    }
}
