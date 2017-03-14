package org.infobip.mobile.messaging.geo;

import android.os.Bundle;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;

import java.util.List;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReport {

    private Area area;
    private GeoEventType event;
    private String campaignId;
    private String signalingMessageId;
    private String messageId;
    private Long timestampOccurred;
    private GeoLatLng triggeringLocation;

    public GeoReport() {

    }

    public GeoReport(String campaignId, String messageId, String signalingMessageId, GeoEventType event, Area area, Long timestampOccurred, GeoLatLng triggeringLocation) {
        this.area = area;
        this.campaignId = campaignId;
        this.signalingMessageId = signalingMessageId;
        this.messageId = messageId;
        this.event = event;
        this.timestampOccurred = timestampOccurred;
        this.triggeringLocation = triggeringLocation;
    }

    public static List<GeoReport> createFrom(Bundle bundle) {
        return BundleMapper.geoReportsFromBundle(bundle);
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public GeoEventType getEvent() {
        return event;
    }

    public void setEvent(GeoEventType event) {
        this.event = event;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getSignalingMessageId() {
        return signalingMessageId;
    }

    public void setSignalingMessageId(String signalingMessageId) {
        this.signalingMessageId = signalingMessageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getTimestampOccurred() {
        return timestampOccurred;
    }

    public void setTimestampOccurred(Long timestampOccurred) {
        this.timestampOccurred = timestampOccurred;
    }

    public GeoLatLng getTriggeringLocation() {
        return triggeringLocation;
    }

    public void setTriggeringLocation(GeoLatLng triggeringLocation) {
        this.triggeringLocation = triggeringLocation;
    }
}
