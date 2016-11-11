package org.infobip.mobile.messaging.geo;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.ISO8601DateParseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author pandric
 * @since 14.06.2016.
 */
public class Geo implements Parcelable {

    @SerializedName("triggeringLatitude")
    private Double triggeringLatitude;

    @SerializedName("triggeringLongitude")
    private Double triggeringLongitude;

    @SerializedName("geo")
    private List<Area> areasList = new ArrayList<>();

    @SerializedName("deliveryTime")
    private DeliveryTime deliveryTime;

    @SerializedName("event")
    private List<GeoEvent> events = new ArrayList<>();

    @SerializedName("expiryTime")
    private String expiryTime;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("campaignId")
    private String campaignId;

    protected Geo(Double triggeringLatitude, Double triggeringLongitude, List<Area> areasList) {
        this.triggeringLatitude = triggeringLatitude;
        this.triggeringLongitude = triggeringLongitude;
        this.areasList = areasList;
    }

    protected Geo(Parcel in) {
        this.triggeringLatitude = in.readDouble();
        this.triggeringLongitude = in.readDouble();
        in.readTypedList(this.areasList, Area.CREATOR);
        this.deliveryTime = in.readParcelable(DeliveryTime.class.getClassLoader());
        in.readTypedList(this.events, GeoEvent.CREATOR);
        this.expiryTime = in.readString();
        this.startTime = in.readString();
        this.campaignId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(triggeringLatitude);
        parcel.writeDouble(triggeringLongitude);
        parcel.writeTypedList(areasList);
        parcel.writeParcelable(deliveryTime, i);
        parcel.writeTypedList(events);
        parcel.writeString(expiryTime);
        parcel.writeString(startTime);
        parcel.writeString(campaignId);
    }

    public static final Creator<Geo> CREATOR = new Creator<Geo>() {
        @Override
        public Geo createFromParcel(Parcel in) {
            return new Geo(in);
        }

        @Override
        public Geo[] newArray(int size) {
            return new Geo[size];
        }
    };

    public static Geo createFrom(Bundle bundle) {
        return bundle.getParcelable(BroadcastParameter.EXTRA_GEOFENCE_AREAS);
    }

    public Double getTriggeringLatitude() {
        return triggeringLatitude;
    }

    public Double getTriggeringLongitude() {
        return triggeringLongitude;
    }

    public List<Area> getAreasList() {
        return areasList;
    }

    protected List<GeoEvent> getEvents() {
        return events;
    }

    protected DeliveryTime getDeliveryTime() {
        return deliveryTime;
    }

    protected Date getExpiryDate() {
        try {
            return DateTimeUtil.ISO8601DateFromString(expiryTime);
        } catch (ISO8601DateParseException e) {
            Log.e(MobileMessaging.TAG, "Cannot parse expiry date: " + e.getMessage());
            Log.d(MobileMessaging.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    protected Date getStartDate() {
        try {
            return DateTimeUtil.ISO8601DateFromString(startTime);
        } catch (ISO8601DateParseException e) {
            Log.e(MobileMessaging.TAG, "Cannot parse start date: " + e.getMessage());
            Log.d(MobileMessaging.TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    protected String getCampaignId() {
        return campaignId;
    }

    /**
     * Checks that monitoring can be activated for this geo campaign
     *
     * @return true if geo campaign can be monitored
     */
    boolean isEligibleForMonitoring() {
        Date now = new Date();
        return (getStartDate() == null || getStartDate().before(now)) &&
                !isExpired();
    }

    /**
     * Checks if this geo campaing is expired
     *
     * @return true if geo campaign is expired
     */
    boolean isExpired() {
        Date now = new Date();
        Date expiryDate = getExpiryDate();
        return expiryDate != null && expiryDate.before(now);
    }
}