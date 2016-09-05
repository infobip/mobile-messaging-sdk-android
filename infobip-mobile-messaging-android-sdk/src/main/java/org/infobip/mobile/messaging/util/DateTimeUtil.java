package org.infobip.mobile.messaging.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author sslavin
 * @since 01/09/16.
 */
public class DateTimeUtil {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String GMT_TIME_ZONE = "+00:00";
    private static final String ISO8601_GMT_Z_MATCHER = "Z$";

    /**
     * Android's SimpleDateFormat cannot properly parse 'Z' (ISO8601 GMT) time zone.
     * </p>
     * This method does additional job and replaces 'Z' with '+00:00'.
     *
     * @param dateString string representation of date
     * @return Date object
     */
    public static Date ISO8601DateFromString(String dateString) {
        if (dateString == null) {
            return null;
        }

        String date = dateString.trim().replaceAll(ISO8601_GMT_Z_MATCHER, GMT_TIME_ZONE);
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
