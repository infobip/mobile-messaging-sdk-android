package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.LocalEvent;
import org.infobip.mobile.messaging.geo.geofencing.Geofencing;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class MobileGeoImpl extends MobileGeo {

    private static MobileGeoImpl instance;
    private final Context context;
    private final GeoReportSynchronizationReceiver geoReportSynchronizationReceiver;
    private Geofencing geofencing;

    public static MobileGeoImpl getInstance(Context context) {
        if (instance == null) {
            instance = new MobileGeoImpl(context);
        }
        return instance;
    }

    private MobileGeoImpl(Context context) {
        this.context = context;
        geoReportSynchronizationReceiver = new GeoReportSynchronizationReceiver();
    }

    @Override
    public void activateGeofencing() {
        activateGeofencing(Geofencing.getInstance(context));
    }

    void activateGeofencing(Geofencing geofencing) {
        this.geofencing = geofencing;
        if (geofencing == null) return;

        GeofencingHelper.setActivated(context, true);
        geofencing.setGeoComponentsEnabledSettings(context, true);
        geofencing.startGeoMonitoring();
        enableGeoReportSynchronization(true);
    }

    @Override
    public void deactivateGeofencing() {
        deactivateGeofencing(this.geofencing);
        this.geofencing = null;
    }

    void deactivateGeofencing(Geofencing geofencing) {
        if (geofencing == null) {
            geofencing = Geofencing.getInstance(context);
        }

        GeofencingHelper.setActivated(context, false);
        geofencing.setGeoComponentsEnabledSettings(context, false);
        geofencing.stopGeoMonitoring();
        enableGeoReportSynchronization(false);
    }

    private void enableGeoReportSynchronization(boolean enable) {
        if (enable) {
            LocalBroadcastManager.getInstance(context).registerReceiver(geoReportSynchronizationReceiver, new IntentFilter(LocalEvent.APPLICATION_FOREGROUND.getKey()));
        } else {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(geoReportSynchronizationReceiver);
        }
    }

    @Override
    public boolean isGeofencingActivated() {
        return GeofencingHelper.isActivated(context);
    }

    @Override
    public void cleanup() {
        deactivateGeofencing();
        Geofencing.getInstance(context).cleanup();

        PreferenceHelper.remove(context, MobileMessagingGeoProperty.ALL_ACTIVE_GEO_AREAS_MONITORED.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.GEOFENCING_ACTIVATED.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey());
    }
}
