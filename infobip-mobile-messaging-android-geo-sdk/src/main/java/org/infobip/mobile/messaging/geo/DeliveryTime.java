package org.infobip.mobile.messaging.geo;

/**
 * @author sslavin
 * @since 17/10/2016.
 */

public class DeliveryTime {
    private String days;
    private String timeInterval;

    public DeliveryTime(String days, String timeInterval) {
        this.days = days;
        this.timeInterval = timeInterval;
    }

    public String getDays() {
        return days;
    }

    public String getTimeInterval() {
        return timeInterval;
    }
}