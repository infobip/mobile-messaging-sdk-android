package org.infobip.mobile.messaging;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.infobip.mobile.messaging.geo.GeofenceTransitionsIntentService;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * @author pandric
 * @since 03.06.2016.
 */
public class Geofencing implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static String TAG = MobileMessaging.TAG + "/Geofencing";

    private static Context context;
    private static Geofencing instance;
    private GoogleApiClient googleApiClient;
    private List<Geofence> geofences;
    private PendingIntent geofencePendingIntent;

    private Geofencing(Context context) {
        Geofencing.context = context;
        geofences = getGeofences(context);
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public static Geofencing getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        instance = new Geofencing(context);
        return instance;
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                    activate();
                }
            }
        }
    }

    void activate() {
        checkRequiredService(context, GeofenceTransitionsIntentService.class);
        if (MobileMessagingCore.isGeofencingActivated(context)) {
            addGeofenceAreasToPlayServices();
            activateGeofences();
        }
    }

    void addGeofenceAreasToPlayServices() {
        List<Message> messages = MobileMessagingCore.getInstance(context).getMessageStore().bind(context);
        if (!messages.isEmpty()) {
            this.geofences.clear();
            for (Message message : messages) {
                List<GeofenceAreas.Area> geoAreasList = message.getGeofenceAreasList();

                if (geoAreasList != null && !geoAreasList.isEmpty()) {
                    for (GeofenceAreas.Area area : geoAreasList) {
                        this.geofences.add(area.toGeofence());
                    }
                }
            }
        }
    }

    private void activateGeofences() {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("You shall provide instance of Activity as context for geofencing!");
        }

        Activity activity = (Activity) context;
        if (ActivityCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            String[] permissions = {ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(activity, permissions, ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        if (geofences.isEmpty()) {
            Log.d(TAG, "Skip adding geofences. No geofence areas added.");
            return;
        }

        LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest(), geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        MobileMessagingCore.setGeofencingActivated(context, status.isSuccess());
                        logGeofenceStatus(status, true);
                        }
                    });
    }


    void deactivate() {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("You shall provide instance of Activity as context for geofencing!");
        }

        Activity activity = (Activity) context;
        if (ActivityCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            String[] permissions = {ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(activity, permissions, ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        MobileMessagingCore.setGeofencingActivated(context, !status.isSuccess());
                        logGeofenceStatus(status, false);
                    }
                });
    }

    /**
     * Checks if required service is defined.
     *
     * @param serviceClass Class of the service to be checked.
     * @throws ConfigurationException
     */
    private void checkRequiredService(Context context, Class serviceClass) {
        String serviceName = serviceClass.getCanonicalName();
        ComponentName componentName = new ComponentName(context.getPackageName(), serviceName);
        try {
            context.getPackageManager().getServiceInfo(componentName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException localNameNotFoundException) {
            throw new ConfigurationException(String.format(ConfigurationException.Reason.MISSING_REQUIRED_SERVICE.message(), serviceName));
        }
    }


    private void logGeofenceStatus(@NonNull Status status, boolean activated) {
        if (status.isSuccess()) {
            Log.d(TAG, "Geofencing monitoring " + (activated ? "" : "de-") + "activated successfully");

        } else {
            Log.e(TAG, "Geofencing monitoring " +  (activated ? "" : "de-") + "activation failed", new Throwable(status.getStatusMessage()));
        }
    }

    private GeofencingRequest geofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent geofencePendingIntent() {
        if (geofencePendingIntent == null) {
            Intent intent = new Intent(context, GeofenceTransitionsIntentService.class);
            geofencePendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return geofencePendingIntent;
    }

    private List<Geofence> getGeofences(Context context) {
        SharedPreferencesMessageStore messageStore = new SharedPreferencesMessageStore();
        List<Message> messages = messageStore.bind(context);
        List<Geofence> geofences = new ArrayList<>(messages.size());

        if (!messages.isEmpty()) {
            for (Message message : messages) {
                List<GeofenceAreas.Area> geoAreasList = message.getGeofenceAreasList();

                if (geoAreasList != null && !geoAreasList.isEmpty()) {
                    for (GeofenceAreas.Area area : geoAreasList) {
                        geofences.add(area.toGeofence());
                    }
                }
            }
        }

        return geofences;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");
        activateGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorMessage(), new Throwable(connectionResult.getErrorMessage()));
    }
}
