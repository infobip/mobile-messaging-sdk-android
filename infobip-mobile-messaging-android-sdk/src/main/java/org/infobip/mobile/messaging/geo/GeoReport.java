package org.infobip.mobile.messaging.geo;

import android.os.Bundle;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;

import java.util.List;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReport {

    public static class GeoLatLng {
        private Double lat;
        private Double lng;

        GeoLatLng(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLng() {
            return lng;
        }
    }

    private Area area;
    private GeoEventType event;
    private String campaignId;
    private String signalingMessageId;
    private String messageId;
    private Long timestampOccurred;
    private GeoLatLng triggeringLatLng;

    protected GeoReport(String campaignId, String messageId, String signalingMessageId, GeoEventType event, Area area, Long timestampOccurred, GeoLatLng triggeringLatLng) {
        this.area = area;
        this.campaignId = campaignId;
        this.signalingMessageId = signalingMessageId;
        this.messageId = messageId;
        this.event = event;
        this.timestampOccurred = timestampOccurred;
        this.triggeringLatLng = triggeringLatLng;
    }

    public static List<GeoReport> createFrom(Bundle bundle) {
        return BundleMapper.geoReportsFromBundle(bundle);
    }

    public String getMessageId() {
        return messageId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public GeoEventType getEvent() {
        return event;
    }

    public Area getArea() {
        return area;
    }

    public Long getTimestampOccurred() {
        return timestampOccurred;
    }

    public GeoLatLng getTriggeringLocation() {
        return triggeringLatLng;
    }

    public String getSignalingMessageId() {
        return signalingMessageId;
    }
}
