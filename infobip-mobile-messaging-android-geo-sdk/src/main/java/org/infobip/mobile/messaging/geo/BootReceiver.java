package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;

/**
 * @author sslavin
 * @since 09/09/16.
 */
public class BootReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MobileMessagingLogger.i("Received boot completed intent");
        GeofencingHelper geofencingHelper = new GeofencingHelper(context);
        geofencingHelper.handleBootCompleted();
    }
}
