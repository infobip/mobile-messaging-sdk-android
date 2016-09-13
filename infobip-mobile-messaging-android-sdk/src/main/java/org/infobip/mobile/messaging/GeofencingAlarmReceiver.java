package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author sslavin
 * @since 09/09/16.
 */
public class GeofencingAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Geofencing.getInstance(context).activate();
    }
}
