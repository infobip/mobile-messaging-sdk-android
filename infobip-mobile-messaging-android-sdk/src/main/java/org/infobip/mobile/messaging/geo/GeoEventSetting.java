package org.infobip.mobile.messaging.geo;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class GeoEventSetting {

    public static final int UNLIMITED_RECURRING = 0;

    private GeoEventType type;
    private Integer limit;
    private Long timeoutInMinutes;

    GeoEventSetting(GeoEventType type, Integer limit, Long timeoutInMinutes) {
        this.type = type;
        this.limit = limit;
        this.timeoutInMinutes = timeoutInMinutes;
    }

    public int getLimit() {
        return limit;
    }

    public long getTimeoutInMinutes() {
        return timeoutInMinutes;
    }

    public GeoEventType getType() {
        return type;
    }
}
