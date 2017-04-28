package org.infobip.mobile.messaging.geo;


import android.support.v4.util.ArraySet;

public enum MobileMessagingGeoProperty {

    FINISHED_CAMPAIGN_IDS("org.infobip.mobile.messaging.infobip.FINISHED_CAMPAIGN_IDS", new ArraySet<>()),
    SUSPENDED_CAMPAIGN_IDS("org.infobip.mobile.messaging.infobip.SUSPENDED_CAMPAIGN_IDS", new ArraySet<>()),

    ALL_ACTIVE_GEO_AREAS_MONITORED("org.infobip.mobile.messaging.infobip.ALL_ACTIVE_GEO_AREAS_MONITORED", false),
    UNREPORTED_GEO_EVENTS("org.infobip.mobile.messaging.infobip.UNREPORTED_GEO_EVENTS", new String[0]),
    GEOFENCING_ACTIVATED("org.infobip.mobile.messaging.geo.GEOFENCING_ACTIVATED", false);

    private final String key;
    private final Object defaultValue;

    MobileMessagingGeoProperty(String key, Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
