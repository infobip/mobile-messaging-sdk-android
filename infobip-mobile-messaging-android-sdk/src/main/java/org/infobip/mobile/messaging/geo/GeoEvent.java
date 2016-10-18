package org.infobip.mobile.messaging.geo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class GeoEvent implements Parcelable {

    public static final int UNLIMITED_RECURRING = 0;

    @SerializedName("type")
    private String type;

    @SerializedName("limit")
    private Integer limit;

    @SerializedName("timeoutInMinutes")
    private Long timeoutInMinutes;

    public GeoEvent(String type, Integer limit, Long timeoutInMinutes) {
        this.type = type;
        this.limit = limit;
        this.timeoutInMinutes = timeoutInMinutes;
    }

    public GeoEvent(Parcel parcel) {
        this.type = parcel.readString();
        this.limit = parcel.readInt();
        this.timeoutInMinutes = parcel.readLong();
    }

    public int getLimit() {
        return limit;
    }

    public long getTimeoutInMinutes() {
        return timeoutInMinutes;
    }

    public String getType() {
        return type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(type);
        parcel.writeInt(limit);
        parcel.writeLong(timeoutInMinutes);
    }

    public static final Creator<GeoEvent> CREATOR = new Creator<GeoEvent>() {
        @Override
        public GeoEvent createFromParcel(Parcel parcel) {
            return new GeoEvent(parcel);
        }

        @Override
        public GeoEvent[] newArray(int size) {
            return new GeoEvent[size];
        }
    };
}
