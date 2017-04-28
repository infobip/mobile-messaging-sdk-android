package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.LocalEvent;

public class GeoReportSynchronizationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equalsIgnoreCase(LocalEvent.APPLICATION_FOREGROUND.getKey())) {
            GeoReportSynchronization geoReportSynchronization = new GeoReportSynchronization(context);
            geoReportSynchronization.synchronize();
        }
    }
}
