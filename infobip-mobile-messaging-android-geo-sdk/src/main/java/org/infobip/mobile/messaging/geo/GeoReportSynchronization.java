package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.platform.AndroidGeoBroadcaster;
import org.infobip.mobile.messaging.geo.report.GeoReporter;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;

class GeoReportSynchronization {

    private final GeoReporter geoReporter;

    GeoReportSynchronization(Context context) {
        MobileMessagingCore core = MobileMessagingCore.getInstance(context);
        this.geoReporter = new GeoReporter(context, core, new AndroidGeoBroadcaster(context),
                core.getStats(), new MobileApiResourceProvider().getMobileApiGeo(context));
    }

    void synchronize() {
        geoReporter.synchronize();
    }
}
