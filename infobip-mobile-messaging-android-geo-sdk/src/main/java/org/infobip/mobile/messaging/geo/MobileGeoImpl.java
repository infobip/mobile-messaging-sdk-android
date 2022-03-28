package org.infobip.mobile.messaging.geo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.geofencing.Geofencing;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.permissions.GeoPermissionsRequestManager;
import org.infobip.mobile.messaging.geo.push.PushMessageHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MobileGeoImpl extends MobileGeo implements MessageHandlerModule, GeoPermissionsRequestManager.PermissionsRequester {

    @SuppressLint("StaticFieldLeak")
    private static MobileGeoImpl instance;
    private Context context;
    private Geofencing geofencing;
    private GeoPermissionsRequestManager permissionsRequestManager;
    private boolean shouldShowPermissionsDialogIfShownOnce = false;

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
    public void depersonalize() {
        Geofencing.getInstance(context).depersonalize();

        PreferenceHelper.remove(context, MobileMessagingGeoProperty.ALL_ACTIVE_GEO_AREAS_MONITORED.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey());
        PreferenceHelper.remove(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey());
    }

    @Override
    public void performSyncActions() {
        // do nothing
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
        MobileMessagingLogger.d("Message with id: " + message.getMessageId() + " will be handled by Geo MessageHandler");
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

    @SuppressLint("MissingPermission")
    public void activateGeofencingWithAutomaticPermissionsRequest(boolean shouldShowPermissionsNotGrantedDialogIfShownOnce) {
        if (!requestPermissionsIfNeeded(shouldShowPermissionsNotGrantedDialogIfShownOnce)) {
            MobileMessagingLogger.e("[Geofencing Demo] Permissions required for geofencing weren't granted");
            return;
        }
        activateGeofencing(Geofencing.getInstance(context));
    }

    /* PermissionsRequestManager.PermissionsRequester */

    @Override
    public void setContextForRequestingPermissions(AppCompatActivity activity) {
        this.permissionsRequestManager = new GeoPermissionsRequestManager(activity, this);
    }

    @Override
    public void setContextForRequestingPermissions(Fragment fragment) {
        this.permissionsRequestManager = new GeoPermissionsRequestManager(fragment, this);
    }

    private boolean requestPermissionsIfNeeded(boolean shouldShowPermissionsDialogIfShownOnce) {
        if (permissionsRequestManager == null) {
            MobileMessagingLogger.e("[Geofencing] setContextForRequestingPermissions wasn't called");
            return false;
        }
        this.shouldShowPermissionsDialogIfShownOnce = shouldShowPermissionsDialogIfShownOnce;
        return permissionsRequestManager.isRequiredPermissionsGranted();
    }

    @Override
    public void onPermissionGranted() {
        if (permissionsRequestManager == null || !permissionsRequestManager.isRequiredPermissionsGranted()) {
            MobileMessagingLogger.e("[Geofencing] Permissions required for geofencing weren't granted" + Arrays.toString(requiredGeoPermissions()));
            return;
        }
        activateGeofencing(Geofencing.getInstance(context));
    }

    @NonNull
    @Override
    public String[] requiredPermissions() {
        return requiredGeoPermissions();
    }

    @Override
    public boolean shouldShowPermissionsNotGrantedDialogIfShownOnce() {
        return shouldShowPermissionsDialogIfShownOnce;
    }

    @Override
    public int permissionsNotGrantedDialogTitle() {
        return R.string.geofencing_permissions_not_granted_title;
    }

    @Override
    public int permissionsNotGrantedDialogMessage() {
        return R.string.geofencing_permissions_not_granted_message;
    }

    private String[] requiredGeoPermissions() {
        String[] permissions = new String[0];
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            } else  if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            } else {
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            }
        }
        return permissions;
    }
}
