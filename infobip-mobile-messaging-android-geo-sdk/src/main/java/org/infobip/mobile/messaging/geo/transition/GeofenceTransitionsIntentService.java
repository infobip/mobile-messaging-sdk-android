package org.infobip.mobile.messaging.geo.transition;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;

import static org.infobip.mobile.messaging.MobileMessagingJob.GEO_TRANSITION_JOB_ID;
import static org.infobip.mobile.messaging.MobileMessagingJob.getScheduleId;

public class GeofenceTransitionsIntentService extends JobIntentService {

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, GeofenceTransitionsIntentService.class, getScheduleId(context, GEO_TRANSITION_JOB_ID), work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final GeoAreasHandler geoAreasHandler = new GeoAreasHandler(this, new AndroidGeoBroadcaster(this));
        geoAreasHandler.handleTransition(intent);
    }
}
