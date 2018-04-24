package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.geofencing.Geofencing;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.push.PushMessageHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MobileGeoImpl extends MobileGeo implements MessageHandlerModule {

    private static MobileGeoImpl instance;
    private Context context;
    private Geofencing geofencing;

    public static MobileGeoImpl getInstance(Context context) {
        if (instance == null) {
            instance = MobileMessagingCore.getInstance(context).getMessageHandlerModule(MobileGeoImpl.class);
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

        GeofencingHelper.setGeoActivated(context, true);
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

        GeofencingHelper.setGeoActivated(context, false);
        geofencing.setGeoComponentsEnabledSettings(context, false);
        geofencing.stopGeoMonitoring();
    }

    @Override
    public boolean isGeofencingActivated() {
        return GeofencingHelper.isGeoActivated(context);
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
    public void logoutUser() {
        Geofencing.getInstance(context).logoutUser();

        PreferenceHelper.remove(context, MobileMessagingGeoProperty.ALL_ACTIVE_GEO_AREAS_MONITORED.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey());
    }

    @Override
    public void init(Context appContext) {
        this.context = appContext;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!hasGeo(message)) {
            return false;
        }

        PushMessageHandler pushMessageHandler = new PushMessageHandler();
        pushMessageHandler.handleGeoMessage(context, message);
        return true;
    }

    @Override
    public boolean messageTapped(Message message) {
        return false;
    }

    @Override
    public void applicationInForeground() {
        GeoReportSynchronization geoReportSynchronization = new GeoReportSynchronization(context);
        geoReportSynchronization.synchronize();
    }

    private static boolean hasGeo(Message message) {
        if (message == null || message.getInternalData() == null) {
            return false;
        }

        try {
            JSONObject geo = new JSONObject(message.getInternalData());
            JSONArray areas = geo.optJSONArray("geo");
            return areas != null && areas.length() > 0;
        } catch (JSONException e) {
            MobileMessagingLogger.e(e.getMessage());
            return false;
        }
    }
}
