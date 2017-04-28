package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReporter;

class GeoReportSynchronization {

    private final GeoReporter geoReporter;

    GeoReportSynchronization(Context context) {
        this.geoReporter = new GeoReporter(context, new AndroidGeoBroadcaster(context), MobileMessagingCore.getInstance(context).getStats());
    }

    void synchronize() {
        geoReporter.synchronize();
    }
}
