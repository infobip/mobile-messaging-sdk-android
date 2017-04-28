package org.infobip.mobile.messaging.geo.platform;


import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoMessage;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.List;

public interface GeoBroadcaster {

    /**
     * Sends geo occured broadcast
     *
     * @param event      event type
     * @param geoMessage generated message with geo data information
     */
    void geoEvent(GeoEventType event, GeoMessage geoMessage);

    /**
     * Sends broadcast that geo events were reported to the server
     *
     * @param reports geo reports.
     */
    void geoReported(List<GeoReport> reports);

    /**
     * Sends broadcast that error occured.
     *
     * @param error error to provide.
     */
    void error(MobileMessagingError error);
}
