package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReporter;

class GeoReportSynchronization {

    private final GeoReporter geoReporter;

    GeoReportSynchronization(Context context) {
        MobileMessagingCore core = MobileMessagingCore.getInstance(context);
        this.geoReporter = new GeoReporter(context, core, new AndroidGeoBroadcaster(context), core.getStats());
    }

    void synchronize() {
        geoReporter.synchronize();
    }
}
