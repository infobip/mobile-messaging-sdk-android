package org.infobip.mobile.messaging.geo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.geo.ConfigurationException.Reason;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pandric
 * @since 03.06.2016.
 */
public class Geofencing implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static String TAG = "Geofencing";

    private static Context context;
    private static Geofencing instance;
    private GoogleApiClient googleApiClient;
    private List<Geofence> geofences;
    private PendingIntent geofencePendingIntent;
    private MessageStore messageStore;

    private Geofencing(Context context) {
        checkRequiredService(context, GeofenceTransitionsIntentService.class);

        Geofencing.context = context;
        geofences = new ArrayList<>();
        messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
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

    public static void scheduleRefresh(Context context) {
        scheduleRefresh(context, new Date());
    }

    public static void scheduleRefresh(Context context, Date when) {
        MobileMessagingLogger.i(TAG, "Next refresh in: " + when);

        if (when == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GeofencingConsistencyReceiver.class);
        intent.setAction(GeofencingConsistencyReceiver.SCHEDULED_GEO_REFRESH_ACTION);
        alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), PendingIntent.getBroadcast(context, 0, intent, 0));
    }

    @SuppressWarnings("WeakerAccess")
    static Tuple<List<Geofence>, Date> calculateGeofencesToMonitorAndNextCheckDate(MessageStore messageStore) {
        Date nextCheckDate = null;
        Map<String, Geofence> geofences = new HashMap<>();
        Map<String, Date> expiryDates = new HashMap<>();
        List<Message> messages = messageStore.findAll(context);

        for (Message message : messages) {
            Geo geo = message.getGeo();
            if (geo == null || geo.getAreasList() == null || geo.getAreasList().isEmpty()) {
                continue;
            }

            final Set<String> finishedCampaignIds = MobileMessagingCore.getInstance(context).getFinishedCampaignIds();
            if (finishedCampaignIds.contains(geo.getCampaignId())) {
                continue;
            }

            if (geo.isEligibleForMonitoring()) {
                List<Area> geoAreasList = message.getGeo().getAreasList();
                for (Area area : geoAreasList) {
                    if (!area.isValid()) {
                        continue;
                    }

                    Date expiry = expiryDates.get(area.getId());
                    if (expiry != null && expiry.after(geo.getExpiryDate())) {
                        continue;
                    }

                    expiryDates.put(area.getId(), geo.getExpiryDate());
                    geofences.put(area.getId(), area.toGeofence(geo.getExpiryDate()));
                }
            }

            nextCheckDate = calculateNextCheckDateForGeo(geo, nextCheckDate);
        }

        List<Geofence> geofenceList = new ArrayList<>(geofences.values());
        return new Tuple<>(geofenceList, nextCheckDate);
    }

    private static Date calculateNextCheckDateForGeo(Geo geo, Date oldCheckDate) {
        Date now = new Date();
        Date expiryDate = geo.getExpiryDate();
        if (expiryDate != null && expiryDate.before(now)) {
            return oldCheckDate;
        }

        Date startDate = geo.getStartDate();
        if (startDate == null || startDate.before(now)) {
            return oldCheckDate;
        }

        if (oldCheckDate != null && oldCheckDate.before(startDate)) {
            return oldCheckDate;
        }

        return startDate;
    }

    @SuppressWarnings("MissingPermission")
    public void startGeoMonitoring() {

        if (!PlayServicesSupport.isPlayServicesAvailable(context) ||
                !MobileMessagingCore.isGeofencingActivated(context)) {
            return;
        }

        if (!checkRequiredPermissions()) {
            return;
        }

        Tuple<List<Geofence>, Date> tuple = calculateGeofencesToMonitorAndNextCheckDate(messageStore);
        scheduleRefresh(context, tuple.getRight());

        geofences = tuple.getLeft();
        if (geofences.isEmpty()) {
            return;
        }

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest(), geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        logGeofenceStatus(status, true);
                    }
                });
    }

    public void stopGeoMonitoring() {

        if (!checkRequiredPermissions()) {
            return;
        }

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        logGeofenceStatus(status, false);
                    }
                });
    }

    /**
     * Has an async call to {@link GoogleApiClient#connect()}
     *
     * @param geofenceRequestIds
     */
    public void removeGeofencesFromMonitoring(List<String> geofenceRequestIds) {
        if (!checkRequiredPermissions()) {
            return;
        }

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofenceRequestIds);
    }

    private boolean checkRequiredPermissions() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MobileMessagingLogger.e("Unable to initialize geofencing", new ConfigurationException(Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION));
            return false;
        }

        return true;
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
            throw new ConfigurationException(Reason.MISSING_REQUIRED_SERVICE, serviceName);
        }
    }

    private void logGeofenceStatus(@NonNull Status status, boolean activated) {
        if (status.isSuccess()) {
            MobileMessagingLogger.d(TAG, "Geofencing monitoring " + (activated ? "" : "de-") + "activated successfully");

        } else {
            MobileMessagingLogger.e(TAG, "Geofencing monitoring " + (activated ? "" : "de-") + "activation failed", new Throwable(status.getStatusMessage()));
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        MobileMessagingLogger.d(TAG, "GoogleApiClient connected");
        startGeoMonitoring();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        MobileMessagingLogger.e(TAG, connectionResult.getErrorMessage(), new ConfigurationException(Reason.CHECK_LOCATION_SETTINGS));
    }
}
