package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * @author sslavin
 * @since 09/09/16.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MobileMessagingLogger.i("Received boot completed intent");
        GeofencingHelper geofencingHelper = new GeofencingHelper(context);
        geofencingHelper.handleBootCompleted();
    }
}
