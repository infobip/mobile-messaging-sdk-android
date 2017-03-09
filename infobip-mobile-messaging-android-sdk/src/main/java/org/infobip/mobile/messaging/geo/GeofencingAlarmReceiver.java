package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @deprecated Use {@link GeofencingConsistencyReceiver} instead.
 * @author sslavin
 * @since 09/09/16.
 */
@Deprecated
public class GeofencingAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Geofencing.getInstance(context).startGeoMonitoring();
    }
}
