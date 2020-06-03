package org.infobip.mobile.messaging.geo.platform;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.geo.GeoEvent;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoMessage;
import org.infobip.mobile.messaging.geo.mapper.GeoBundleMapper;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidGeoBroadcaster implements GeoBroadcaster {

    private final Context context;

    private final static Map<GeoEventType, GeoEvent> eventBroadcasts = new HashMap<GeoEventType, GeoEvent>() {{
        put(GeoEventType.entry, GeoEvent.GEOFENCE_AREA_ENTERED);
    }};

    public AndroidGeoBroadcaster(Context context) {
        this.context = context;
    }

    @Override
    public void geoEvent(@NonNull GeoEventType event, @NonNull GeoMessage geoMessage) {
        GeoEvent broadcastEvent = eventBroadcasts.get(event);
        if (broadcastEvent == null) {
            return;
        }

        Intent geofenceIntent = prepareIntent(broadcastEvent);
        geofenceIntent.putExtras(GeoBundleMapper.geoMessageToBundle(geoMessage));
        LocalBroadcastManager.getInstance(context).sendBroadcast(geofenceIntent);
        context.sendBroadcast(geofenceIntent);
    }

    @Override
    public void geoReported(@NonNull List<GeoReport> reports) {
        if (reports.isEmpty()) {
            return;
        }

        Intent geoReportsSent = prepareIntent(GeoEvent.GEOFENCE_EVENTS_REPORTED);
        geoReportsSent.putExtras(GeoBundleMapper.geoReportsToBundle(reports));
        context.sendBroadcast(geoReportsSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(geoReportsSent);
    }

    @Override
    public void error(@NonNull MobileMessagingError error) {
        Intent reportingError = prepareErrorIntent(Event.API_COMMUNICATION_ERROR);
        reportingError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, error);
        context.sendBroadcast(reportingError);
        LocalBroadcastManager.getInstance(context).sendBroadcast(reportingError);
    }

    private Intent prepareIntent(GeoEvent event) {
        return new Intent(event.getKey())
                .setPackage(context.getPackageName());
    }

    private Intent prepareErrorIntent(Event event) {
        return new Intent(event.getKey())
                .setPackage(context.getPackageName());
    }
}
