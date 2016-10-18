package org.infobip.mobile.messaging.geo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class DeliveryTime implements Parcelable {
    @SerializedName("days")
    private String days;

    @SerializedName("timeInterval")
    private String timeInterval;

    public DeliveryTime(String days, String timeInterval) {
        this.days = days;
        this.timeInterval = timeInterval;
    }

    public DeliveryTime(Parcel parcel) {
        this.days = parcel.readString();
        this.timeInterval = parcel.readString();
    }

    public String getDays() {
        return days;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(days);
        parcel.writeString(timeInterval);
    }

    public static final Creator<DeliveryTime> CREATOR = new Creator<DeliveryTime>() {
        @Override
        public DeliveryTime createFromParcel(Parcel parcel) {
            return new DeliveryTime(parcel);
        }

        @Override
        public DeliveryTime[] newArray(int size) {
            return new DeliveryTime[size];
        }
    };
}