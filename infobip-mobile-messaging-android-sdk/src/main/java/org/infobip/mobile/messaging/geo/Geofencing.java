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
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.geo.ConfigurationException.Reason;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        messageStore = new SharedPreferencesMessageStore();
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
        Log.i(TAG, "Next refresh in: " + when);

        if (when == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), PendingIntent.getBroadcast(context, 0, new Intent(context, GeofencingAlarmReceiver.class), 0));
    }

    private static Tuple<List<Geofence>, Date> calculateGeofencesToMonitorAndNextCheckDate(MessageStore messageStore) {
        Date nextCheckDate = null;
        Date now = new Date();
        Map<String, Geofence> geofences = new HashMap<>();
        Map<String, Date> expiryDates = new HashMap<>();
        List<Message> messages = messageStore.bind(context);

        for (Message message : messages) {
            Geo geo = message.getGeo();
            if (geo == null || geo.getAreasList() == null || geo.getAreasList().isEmpty()) {
                continue;
            }

            if (!geo.isEligibleForMonitoring()) {
                continue;
            }

            final List<String> finishedCampaignIds = Arrays.asList(MobileMessagingCore.getInstance(context).getFinishedCampaignIds());
            if (finishedCampaignIds.contains(geo.getCampaignId())) {
                continue;
            }

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

            Date startDate = geo.getStartDate();
            Date expiryDate = geo.getExpiryDate();
            if (nextCheckDate == null) {
                nextCheckDate = startDate;
            } else if (startDate != null && startDate.before(nextCheckDate) &&
                    expiryDate != null && expiryDate.after(now)) {
                nextCheckDate = startDate;
            }
        }

        List<Geofence> geofenceList = new ArrayList<>(geofences.values());
        return new Tuple<>(geofenceList, nextCheckDate);
    }

    @SuppressWarnings("MissingPermission")
    public void activate() {

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
                        MobileMessagingCore.setGeofencingActivated(context, status.isSuccess());
                        logGeofenceStatus(status, true);
                    }
                });


    }

    public void deactivate() {

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
                        MobileMessagingCore.setGeofencingActivated(context, !status.isSuccess());
                        logGeofenceStatus(status, false);
                    }
                });
    }

    /**
     * Has an async call to {@link GoogleApiClient#connect()}
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
            Log.e(MobileMessaging.TAG, "Unable to initialize geofencing. Please, add the following permission to the AndroidManifest.xml: " + Manifest.permission.ACCESS_FINE_LOCATION);
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
            throw new ConfigurationException(String.format(Reason.MISSING_REQUIRED_SERVICE.message(), serviceName));
        }
    }

    private void logGeofenceStatus(@NonNull Status status, boolean activated) {
        if (status.isSuccess()) {
            Log.d(TAG, "Geofencing monitoring " + (activated ? "" : "de-") + "activated successfully");

        } else {
            Log.e(TAG, "Geofencing monitoring " + (activated ? "" : "de-") + "activation failed", new Throwable(status.getStatusMessage()));
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
        Log.d(TAG, "GoogleApiClient connected");
        activate();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.getErrorMessage(), new ConfigurationException(Reason.CHECK_LOCATION_SETTINGS));
    }
}
