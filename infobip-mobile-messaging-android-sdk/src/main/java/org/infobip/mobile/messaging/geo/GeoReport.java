package org.infobip.mobile.messaging.geo;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.infobip.mobile.messaging.BroadcastParameter;

import java.util.List;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReport implements Parcelable {

    private Area area;
    private GeoEventType event;
    private String campaignId;
    private String messageId;
    private Long timestampOccured;

    public GeoReport(String campaignId, String messageId, GeoEventType event, Area area, Long timestampOccured) {
        this.area = area;
        this.campaignId = campaignId;
        this.messageId = messageId;
        this.event = event;
        this.timestampOccured = timestampOccured;
    }

    private GeoReport(Parcel parcel) {
        this.campaignId = parcel.readString();
        this.messageId = parcel.readString();
        this.event = GeoEventType.valueOf(parcel.readString());
        this.area = parcel.readParcelable(Area.class.getClassLoader());
        this.timestampOccured = parcel.readLong();
    }

    public static List<GeoReport> createFrom(Bundle bundle) {
        return bundle.getParcelableArrayList(BroadcastParameter.EXTRA_GEOFENCE_REPORTS);
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

    public Long getTimestampOccured() {
        return timestampOccured;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(campaignId);
        parcel.writeString(messageId);
        parcel.writeString(event.name());
        parcel.writeParcelable(area, i);
        parcel.writeLong(timestampOccured);
    }

    public static final Creator<GeoReport> CREATOR = new Creator<GeoReport>() {
        @Override
        public GeoReport createFromParcel(Parcel in) {
            return new GeoReport(in);
        }

        @Override
        public GeoReport[] newArray(int size) {
            return new GeoReport[size];
        }
    };
}
