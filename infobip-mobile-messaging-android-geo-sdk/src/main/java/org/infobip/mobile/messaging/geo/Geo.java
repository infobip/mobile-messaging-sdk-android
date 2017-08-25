package org.infobip.mobile.messaging.geo;

import android.os.Bundle;
import android.util.Log;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.geo.mapper.GeoBundleMapper;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.ISO8601DateParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author pandric
 * @since 14.06.2016.
 */
public class Geo extends InternalDataMapper.InternalData {

    private Double triggeringLatitude;
    private Double triggeringLongitude;
    private DeliveryTime deliveryTime;
    private String expiryTime;
    private String startTime;
    private String campaignId;
    private List<Area> geo = new ArrayList<>();
    private List<GeoEventSettings> event = new ArrayList<>();

    public Geo(Double triggeringLatitude, Double triggeringLongitude, DeliveryTime deliveryTime, String expiryTime, String startTime, String campaignId,
               List<Area> areasList, List<GeoEventSettings> eventSettings, long sentDateTime, String contentUrl) {
        super(sentDateTime, contentUrl);
        this.triggeringLatitude = triggeringLatitude;
        this.triggeringLongitude = triggeringLongitude;
        this.deliveryTime = deliveryTime;
        this.expiryTime = expiryTime;
        this.startTime = startTime;
        this.campaignId = campaignId;
        this.geo = areasList;
        this.event = eventSettings;
    }

    public static Geo createFrom(Bundle bundle) {
        return GeoBundleMapper.geoFromBundle(bundle);
    }

    public Double getTriggeringLatitude() {
        return triggeringLatitude;
    }

    public Double getTriggeringLongitude() {
        return triggeringLongitude;
    }

    public List<Area> getAreasList() {
        return geo;
    }

    public List<GeoEventSettings> getEvents() {
        return event;
    }

    public Date getExpiryDate() {
        try {
            return DateTimeUtil.ISO8601DateFromString(expiryTime);
        } catch (ISO8601DateParseException e) {
            MobileMessagingLogger.e("Cannot parse expiry date: " + e.getMessage());
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return null;
        }
    }

    /**
     * Checks that monitoring can be activated for this geo campaign
     *
     * @return true if geo campaign can be monitored
     */
    public boolean isEligibleForMonitoring() {
        Date now = new Date();
        return (getStartDate() == null || getStartDate().before(now)) &&
                !isExpired();
    }

    /**
     * Checks if this geo campaign is expired
     *
     * @return true if geo campaign is expired
     */
    public boolean isExpired() {
        Date now = new Date();
        Date expiryDate = getExpiryDate();
        return expiryDate != null && expiryDate.before(now);
    }

    public DeliveryTime getDeliveryTime() {
        return deliveryTime;
    }

    public Date getStartDate() {
        try {
            return DateTimeUtil.ISO8601DateFromString(startTime);
        } catch (ISO8601DateParseException e) {
            MobileMessagingLogger.e("Cannot parse start date: " + e.getMessage());
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return null;
        }
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getExpiryTime() {
        return expiryTime;
    }

    public String getStartTime() {
        return startTime;
    }
}