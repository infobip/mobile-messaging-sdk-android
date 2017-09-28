package org.infobip.mobile.messaging.geo.transition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeofenceTransitionsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofenceTransitionsIntentService.enqueueWork(context, intent);
    }
}
