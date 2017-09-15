package org.infobip.mobile.messaging.geo.geofencing;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.ConfigurationException.Reason;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.BootReceiver;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeofencingConsistencyReceiver;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.push.PushMessageReceiver;
import org.infobip.mobile.messaging.geo.storage.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.geo.transition.GeofenceTransitionsIntentService;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.ComponentUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeofencingImpl extends Geofencing implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static String TAG = "GeofencingImpl";

    private Context context;
    private static GeofencingImpl instance;
    private final GeofencingHelper geofencingHelper;
    private GoogleApiClient googleApiClient;
    private List<Geofence> geofences;
    private PendingIntent geofencePendingIntent;
    private MessageStore messageStore;
    private GoogleApiClientRequestType requestType;

    private enum GoogleApiClientRequestType {
        ADD_GEOFENCES,
        REMOVE_GEOFENCES,
        NONE
    }

    private GeofencingImpl(Context context) {
        this.context = context;
        requestType = GoogleApiClientRequestType.NONE;
        geofences = new ArrayList<>();
        geofencingHelper = new GeofencingHelper(context);
        messageStore = geofencingHelper.getMessageStoreForGeo();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public static GeofencingImpl getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        instance = new GeofencingImpl(context);
        return instance;
    }

    static void scheduleRefresh(Context context) {
        scheduleRefresh(context, new Date());
    }

    private static void scheduleRefresh(Context context, Date when) {
        if (when != null) {
            MobileMessagingLogger.i(TAG, "Next refresh in " + when);
        }

        GeofencingConsistencyReceiver.scheduleConsistencyAlarm(context, AlarmManager.RTC_WAKEUP, when,
                GeofencingConsistencyReceiver.SCHEDULED_GEO_REFRESH_ACTION, 0);
    }

    private static void scheduleExpiry(Context context, Date when) {
        GeofencingConsistencyReceiver.scheduleConsistencyAlarm(context, AlarmManager.RTC_WAKEUP, when,
                GeofencingConsistencyReceiver.SCHEDULED_GEO_EXPIRE_ACTION, 0);
    }

    void removeExpiredAreasFromStorage() {
        GeoSQLiteMessageStore messageStoreForGeo = (GeoSQLiteMessageStore) geofencingHelper.getMessageStoreForGeo();
        List<Message> messages = messageStoreForGeo.findAll(context);
        List<String> messageIdsToDelete = new ArrayList<>(messages.size());
        Date now = new Date();

        for (Message message : messages) {
            Geo geo = GeoDataMapper.geoFromInternalData(message.getInternalData());
            if (geo == null) {
                continue;
            }

            List<Area> areasList = geo.getAreasList();
            Date expiryDate = geo.getExpiryDate();

            if (areasList == null || areasList.isEmpty()) {
                continue;
            }

            for (Area area : areasList) {
                if (!area.isValid() || expiryDate == null) {
                    continue;
                }

                if (expiryDate.before(now)) {
                    messageIdsToDelete.add(message.getMessageId());
                }
            }
        }

        if (!messageIdsToDelete.isEmpty()) {
            messageStoreForGeo.deleteByIds(context, messageIdsToDelete.toArray(new String[]{}));
        }
    }

    @VisibleForTesting
    public Pair<List<Geofence>, Pair<Date, Date>> calculateGeofencesToMonitorDates(MessageStore messageStore) {
        return calculateGeofencesToMonitorAndNextCheckDates(messageStore);
    }

    @SuppressWarnings("WeakerAccess")
    private Pair<List<Geofence>, Pair<Date, Date>> calculateGeofencesToMonitorAndNextCheckDates(MessageStore messageStore) {
        Date nextCheckRefreshDate = null;
        Date nextCheckExpireDate = null;
        Map<String, Geofence> geofences = new HashMap<>();
        Map<String, Date> expiryDates = new HashMap<>();
        List<Message> messages = messageStore.findAll(context);

        for (Message message : messages) {
            Geo geo = GeoDataMapper.geoFromInternalData(message.getInternalData());
            if (geo == null || geo.getAreasList() == null || geo.getAreasList().isEmpty()) {
                continue;
            }

            nextCheckExpireDate = calculateNextCheckDateForGeoExpiry(geo, nextCheckExpireDate);

            final Set<String> finishedCampaignIds = GeofencingHelper.getFinishedCampaignIds(context);
            if (finishedCampaignIds.contains(geo.getCampaignId())) {
                continue;
            }

            if (geo.isEligibleForMonitoring()) {
                List<Area> geoAreasList = geo.getAreasList();
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

            nextCheckRefreshDate = calculateNextCheckDateForGeoStart(geo, nextCheckRefreshDate);
        }

        List<Geofence> geofenceList = new ArrayList<>(geofences.values());
        return new Pair<>(geofenceList, new Pair<>(nextCheckRefreshDate, nextCheckExpireDate));
    }

    private static Date calculateNextCheckDateForGeoStart(Geo geo, Date oldCheckDate) {
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

    private static Date calculateNextCheckDateForGeoExpiry(Geo geo, Date oldCheckDate) {
        Date now = new Date();
        Date expiryDate = geo.getExpiryDate();

        if (expiryDate == null) {
            if (oldCheckDate == null) {
                return null;

            } else {
                return oldCheckDate;
            }
        }

        if (oldCheckDate != null && oldCheckDate.before(expiryDate)) {
            if (oldCheckDate.before(now)) {
                return now;

            } else {
                return oldCheckDate;
            }
        }

        if (expiryDate.before(now)) {
            return now;
        }

        return expiryDate;
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void startGeoMonitoring() {

        if (!PlayServicesSupport.isPlayServicesAvailable(context) ||
                !GeofencingHelper.isActivated(context) ||
                // checking this to avoid multiple activation of geofencing API on Play services
                GeofencingHelper.areAllActiveGeoAreasMonitored(context)) {
            return;
        }

        if (!checkRequiredPermissions()) {
            return;
        }

        Pair<List<Geofence>, Pair<Date, Date>> tuple = calculateGeofencesToMonitorAndNextCheckDates(messageStore);
        Date nextRefreshDate = tuple.second.first;
        Date nextExpireDate = tuple.second.second;

        scheduleRefresh(context, nextRefreshDate);
        scheduleExpiry(context, nextExpireDate);

        geofences = tuple.first;
        if (geofences.isEmpty()) {
            return;
        }

        requestType = GoogleApiClientRequestType.ADD_GEOFENCES;
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest(), geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        logGeofenceStatus(status, true);
                        requestType = GoogleApiClientRequestType.NONE;
                        GeofencingHelper.setAllActiveGeoAreasMonitored(context, status.isSuccess());
                    }
                });
    }

    @Override
    public void stopGeoMonitoring() {

        GeofencingHelper.setAllActiveGeoAreasMonitored(context, false);

        if (!checkRequiredPermissions()) {
            return;
        }

        requestType = GoogleApiClientRequestType.REMOVE_GEOFENCES;
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
            return;
        }

        LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofencePendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        logGeofenceStatus(status, false);
                        requestType = GoogleApiClientRequestType.NONE;
                    }
                });
    }

    @Override
    public void cleanup() {
        setGeoComponentsEnabledSettings(context, false);
        stopGeoMonitoring();
        messageStore.deleteAll(context);
    }

    @Override
    public void setGeoComponentsEnabledSettings(Context context, boolean componentsStateEnabled) {
        ComponentUtil.setState(context, componentsStateEnabled, GeofenceTransitionsIntentService.class);
        ComponentUtil.setState(context, componentsStateEnabled, GeofencingConsistencyReceiver.class);
        ComponentUtil.setState(context, componentsStateEnabled, PushMessageReceiver.class);
        ComponentUtil.setState(context, componentsStateEnabled, BootReceiver.class);
    }

    private boolean checkRequiredPermissions() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MobileMessagingLogger.e("Unable to initialize geofencing", new ConfigurationException(Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION));
            return false;
        }

        return true;
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
        if (GoogleApiClientRequestType.ADD_GEOFENCES.equals(requestType)) {
            startGeoMonitoring();

        } else if (GoogleApiClientRequestType.REMOVE_GEOFENCES.equals(requestType)) {
            stopGeoMonitoring();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        MobileMessagingLogger.e(TAG, connectionResult.getErrorMessage(), new ConfigurationException(Reason.CHECK_LOCATION_SETTINGS));
    }
}
