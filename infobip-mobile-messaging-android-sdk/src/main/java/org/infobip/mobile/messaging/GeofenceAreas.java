package org.infobip.mobile.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.SerializedName;

import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author pandric
 * @since 14.06.2016.
 */
public class GeofenceAreas implements Parcelable {

    @SerializedName("triggeringLatitude")
    private Double triggeringLatitude;

    @SerializedName("triggeringLongitude")
    private Double triggeringLongitude;

    @SerializedName("geo")
    private List<Area> areasList = new ArrayList<>();

    public GeofenceAreas(Double triggeringLatitude, Double triggeringLongitude, List<Area> areasList) {
        this.triggeringLatitude = triggeringLatitude;
        this.triggeringLongitude = triggeringLongitude;
        this.areasList = areasList;
    }

    protected GeofenceAreas(Parcel in) {
        triggeringLatitude = in.readDouble();
        triggeringLongitude = in.readDouble();
        in.readTypedList(areasList, Area.CREATOR);
    }

    public static final Creator<GeofenceAreas> CREATOR = new Creator<GeofenceAreas>() {
        @Override
        public GeofenceAreas createFromParcel(Parcel in) {
            return new GeofenceAreas(in);
        }

        @Override
        public GeofenceAreas[] newArray(int size) {
            return new GeofenceAreas[size];
        }
    };

    public Double getTriggeringLatitude() {
        return triggeringLatitude;
    }

    public Double getTriggeringLongitude() {
        return triggeringLongitude;
    }

    public List<Area> getAreasList() {
        return areasList;
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
    }

    public static class Area implements Parcelable {

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

        @SerializedName("expiryTime")
        private String expiryTime;

        @SerializedName("startTime")
        private String startTime;

        public Area(String id, String title, Double latitude, Double longitude, Integer radius, String expiryTime) {
            this.id = id;
            this.title = title;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
            this.expiryTime = expiryTime;
        }

        protected Area(Parcel in) {
            id = in.readString();
            title = in.readString();
            latitude = in.readDouble();
            longitude = in.readDouble();
            radius = in.readInt();
            expiryTime = in.readString();
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

        public Date getExpiryDate() {
            return DateTimeUtil.ISO8601DateFromString(expiryTime);
        }

        public Date getStartDate() {
            return DateTimeUtil.ISO8601DateFromString(startTime);
        }

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

        public Geofence toGeofence() {
            Long expirationDurationMillis = 0L;
            Date expiryDate = getExpiryDate();
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
            parcel.writeString(expiryTime);
        }

        /**
         * geofence is valid and it can be monitored
         *
         * @return
         */
        public boolean isEligibleForMonitoring() {
            Date now = new Date();
            return getId() != null &&
                    getLatitude() != null &&
                    getLongitude() != null &&
                    getRadius() != null &&
                    (getStartDate() == null || getStartDate().before(now)) &&
                    !isExpired();
        }

        public boolean isExpired() {
            Date now = new Date();
            Date expiryDate = getExpiryDate();
            return expiryDate != null && expiryDate.before(now);
        }
    }
}