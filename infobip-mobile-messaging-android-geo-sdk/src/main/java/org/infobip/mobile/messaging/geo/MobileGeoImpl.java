package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.geofencing.Geofencing;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.push.PushMessageHandler;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class MobileGeoImpl extends MobileGeo implements MessageHandlerModule {

    private static MobileGeoImpl instance;
    private Context context;
    private Geofencing geofencing;

    public static MobileGeoImpl getInstance(Context context) {
        if (instance == null) {
            instance = MobileMessagingCore.getInstance(context).getGeoMessageHandlerModule();
        }
        return instance;
    }

    public MobileGeoImpl() {
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

    @Override
    public void setContext(Context appContext) {
        this.context = appContext;
    }

    @Override
    public void messageReceived(Message message) {
        if (MobileMessagingCore.hasGeo(message)) {
            PushMessageHandler pushMessageHandler = new PushMessageHandler();
            pushMessageHandler.handleGeoMessage(context, message);
        }
    }

    @Override
    public void applicationInForeground() {
        GeoReportSynchronization geoReportSynchronization = new GeoReportSynchronization(context);
        geoReportSynchronization.synchronize();
    }
}
