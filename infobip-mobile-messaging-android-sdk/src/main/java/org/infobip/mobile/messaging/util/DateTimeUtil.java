package org.infobip.mobile.messaging.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

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
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(date);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    /**
     * This method compares ONLY times of two dates. Year, month and day are ignored in this comparison.
     *
     * @return difference of two timestamps
     */
    public static int compareTimes(Date d1, Date d2) {
        int t1 = (int) (d1.getTime() % (24 * 60 * 60 * 1000L));
        int t2 = (int) (d2.getTime() % (24 * 60 * 60 * 1000L));
        return (t1 - t2);
    }

    public static int dayOfWeekISO8601() {
        Calendar calendar = Calendar.getInstance();
        int calendarDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Calendar day numbers, 1 refer to Sunday
        return calendarDayOfWeek == 1 ? 7 : calendarDayOfWeek - 1; // ISO 8601, 1 refers to Monday
    }

    public static boolean isCurrentTimeBetweenDates(String startTime, String endTime) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HHmm", Locale.getDefault());

        Date startDate = simpleDateFormat.parse(startTime);
        Calendar timeStart = Calendar.getInstance();
        timeStart.setTime(startDate);

        Date endDate = simpleDateFormat.parse(endTime);
        Calendar timeEnd = Calendar.getInstance();
        timeEnd.setTime(endDate);

        Date nowDate = new Date();
        return DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
    }
}
