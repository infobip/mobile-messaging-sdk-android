package org.infobip.mobile.messaging.geo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class Area implements Parcelable {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("radiusInMeters")
    private Integer radius;

    public Area(String id, String title, Double latitude, Double longitude, Integer radius) {
        this.id = id;
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    protected Area(Parcel in) {
        id = in.readString();
        title = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        radius = in.readInt();
    }

    public static final Creator<Area> CREATOR = new Creator<Area>() {
        @Override
        public Area createFromParcel(Parcel in) {
            return new Area(in);
        }

        @Override
        public Area[] newArray(int size) {
            return new Area[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getRadius() {
        return radius;
    }

    Geofence toGeofence(Date expiryDate) {
        Long expirationDurationMillis = 0L;
        if (expiryDate != null) {
            expirationDurationMillis = expiryDate.getTime() - System.currentTimeMillis();
        }
        if (expirationDurationMillis <= 0) {
            expirationDurationMillis = Geofence.NEVER_EXPIRE;
        }

        return new Geofence.Builder()
                .setCircularRegion(getLatitude(), getLongitude(), getRadius())
                .setRequestId(getId())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(expirationDurationMillis)
                .build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(title);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeInt(radius);
    }

    /**
     * geofence is valid and it can be monitored
     *
     * @return
     */
    public boolean isValid() {
        return getId() != null &&
                getLatitude() != null &&
                getLongitude() != null &&
                getRadius() != null;
    }
}
