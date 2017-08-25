package org.infobip.mobile.messaging.geo.geofencing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.MobileMessagingGeoProperty;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.storage.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.HashSet;
import java.util.Set;

public class GeofencingHelper {

    private final Context context;
    private GeoSQLiteMessageStore internalStoreForGeo;
    private JsonSerializer serializer = new JsonSerializer(false);

    public GeofencingHelper(Context context) {
        this.context = context;
    }

    @NonNull
    public MessageStore getMessageStoreForGeo() {
        if (internalStoreForGeo == null) {
            internalStoreForGeo = new GeoSQLiteMessageStore();
        }
        return internalStoreForGeo;
    }

    public GeoReport[] removeUnreportedGeoEvents() {
        return PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<GeoReport[]>() {
            @Override
            public GeoReport[] run() {
                String unreportedGeoEventsJsons[] = PreferenceHelper.findStringArray(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey(), new String[0]);
                Set<GeoReport> reports = new HashSet<>();
                for (String unreportedGeoEventJson : unreportedGeoEventsJsons) {
                    try {
                        GeoReport report = serializer.deserialize(unreportedGeoEventJson, GeoReport.class);
                        reports.add(report);
                    } catch (Exception ignored) {
                    }
                }
                PreferenceHelper.remove(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey());
                return reports.toArray(new GeoReport[reports.size()]);
            }
        });
    }

    public void addUnreportedGeoEvents(final GeoReport... reports) {
        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                for (GeoReport report : reports) {
                    PreferenceHelper.appendToStringArray(context, MobileMessagingGeoProperty.UNREPORTED_GEO_EVENTS.getKey(), serializer.serialize(report));
                }
                return null;
            }
        });
    }

    public void removeExpiredAreas() {
        if (isActivated(context) && MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            GeofencingImpl.getInstance(context).removeExpiredAreasFromStorage();
        }
    }

    public void startGeoMonitoringIfNecessary() {
        if (isActivated(context) && MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            GeofencingImpl.getInstance(context).startGeoMonitoring();
        }
    }

    public static void addCampaignStatus(final Context context, final Set<String> finishedCampaignIds, final Set<String> suspendedCampaignIds) {
        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                PreferenceHelper.saveStringSet(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey(),
                        finishedCampaignIds != null ? finishedCampaignIds : new ArraySet<String>());
                PreferenceHelper.saveStringSet(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey(),
                        suspendedCampaignIds != null ? suspendedCampaignIds : new ArraySet<String>());
                return null;
            }
        });
    }

    public void handleBootCompleted() {
        //active areas stop being monitored on boot and we need to re-register them
        setAllActiveGeoAreasMonitored(context, false);
        GeofencingImpl.scheduleRefresh(context);
    }

    public static Set<String> getFinishedCampaignIds(Context context) {
        return PreferenceHelper.findStringSet(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey(), new ArraySet<String>());
    }

    public static Set<String> getSuspendedCampaignIds(Context context) {
        return PreferenceHelper.findStringSet(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey(), new ArraySet<String>());
    }

    public static void setActivated(Context context, boolean activated) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, activated);
    }

    public static boolean isActivated(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED.getKey(), false);
    }

    static boolean areAllActiveGeoAreasMonitored(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingGeoProperty.ALL_ACTIVE_GEO_AREAS_MONITORED.getKey(), false);
    }

    public static void setAllActiveGeoAreasMonitored(Context context, boolean allActiveGeoAreasMonitored) {
        PreferenceHelper.saveBoolean(context, MobileMessagingGeoProperty.ALL_ACTIVE_GEO_AREAS_MONITORED.getKey(), allActiveGeoAreasMonitored);
    }
}
