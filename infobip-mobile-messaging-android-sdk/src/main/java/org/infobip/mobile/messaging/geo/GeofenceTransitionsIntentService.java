package org.infobip.mobile.messaging.geo;

import android.app.IntentService;
import android.content.Intent;

public class GeofenceTransitionsIntentService extends IntentService {


    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final GeoAreasHandler geoAreasHandler = new GeoAreasHandler(this);
        geoAreasHandler.handleTransition(intent);
    }
}
