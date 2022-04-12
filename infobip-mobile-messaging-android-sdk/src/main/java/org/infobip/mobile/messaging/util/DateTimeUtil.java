package org.infobip.mobile.messaging.util;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.platform.Time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author sslavin
 * @since 01/09/16.
 */
public class DateTimeUtil {

    public static final int DATE_TIME_LENGTH_DATE_FORMAT3 = 20;  // example: 2020-02-26T09:41:57Z
    public static final String DATE_YMD_FORMAT = "yyyy-MM-dd";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DATE_FORMAT2 = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final String DATE_FORMAT3 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String GMT_TIME_ZONE = "+00:00";
    private static final String ISO8601_GMT_Z_MATCHER = "Z$";
    private static final String DATE_YMD_HMS_FORMAT = "yy-MM-dd-HH-mm-ss";

    /**
     * Android's SimpleDateFormat cannot properly parse 'Z' (ISO8601 GMT) time zone.
     * <br>
     * This method does additional job and replaces 'Z' with '+00:00'.
     *
     * @param dateString string representation of date
     * @return Date object
     */
    public static Date ISO8601DateFromString(String dateString) throws ISO8601DateParseException {
        if (dateString == null) {
            return null;
        }

        String date = dateString.trim().replaceAll(ISO8601_GMT_Z_MATCHER, GMT_TIME_ZONE);
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(date);
        } catch (Exception ignored) {
            try {
                return new SimpleDateFormat(DATE_FORMAT2, Locale.getDefault()).parse(date);
            } catch (Exception e) {
                throw new ISO8601DateParseException(ISO8601DateParseException.Reason.DATE_PARSE_EXCEPTION, e);
            }
        }
    }

    /**
     * Returns ISO8601-compliant string for the supplied date.
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String ISO8601DateToString(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return simpleDateFormat.format(date);
    }

    /**
     * Returns ISO8601-compliant string in UTC time for the supplied date.
     * 2020-02-26T09:41:57Z
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToISO8601UTCString(Date date) {
        if (date == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        long timeInUtc = date.getTime();
        int offset = calendar.getTimeZone().getOffset(timeInUtc);
        timeInUtc -= offset;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT3, Locale.UK);
        return simpleDateFormat.format(timeInUtc);
    }

    /**
     * Returns ISO8601-compliant string in UTC time for the supplied date.
     * 2020-02-26T09:41:57Z
     *
     * @param dateTime date object
     * @return String representation of Date object
     */
    public static String dateTimeToISO8601UTCString(CustomAttributeValue.DateTime dateTime) {
        return dateToISO8601UTCString(dateTime.getDate());
    }

    /**
     * Returns ISO8601-compliant string for the supplied date.
     * 2020-02-26T09:41:57Z
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToISO8601String(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT3, Locale.UK);
        return simpleDateFormat.format(date.getTime());
    }

    /**
     * Returns ISO8601-compliant string for the supplied date in the current locale.
     * 2020-02-26T09:41:57Z
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToISO8601StringLocale(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT3, Locale.getDefault());
        return simpleDateFormat.format(date.getTime());
    }

    /**
     * Returns ISO8601-compliant string for the supplied date.
     * 2020-02-26T09:41:57Z
     *
     * @param dateTime dateTime object
     * @return String representation of Date object
     */
    public static String dateTimeToISO8601String(CustomAttributeValue.DateTime dateTime) {
        return dateToISO8601String(dateTime.getDate());
    }

    /**
     * Converts "yyyy-MM-dd" string to Date object.
     *
     * @param date string representation of date
     * @return Date object
     */
    public static Date dateFromYMDString(String date) throws ParseException {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(DATE_YMD_FORMAT, Locale.UK).parse(date);
    }

    /**
     * Converts "yyyy-MM-dd" string to Date object in the current locale.
     *
     * @param date string representation of date
     * @return Date object
     */
    public static Date dateFromYMDStringLocale(String date) throws ParseException {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(DATE_YMD_FORMAT, Locale.getDefault()).parse(date);
    }

    /**
     * Converts "yyyy-MM-dd'T'HH:mm:ss'Z'" ISO8601-compliant string to Date object.
     * Example of input: 2020-02-26T09:41:57Z
     *
     * @param date string representation of date
     * @return Date object
     */
    public static Date dateFromISO8601DateUTCString(String date) throws ParseException {
        if (date == null) {
            return null;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT3, Locale.UK);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.parse(date);
    }

    /**
     * Converts "yyyy-MM-dd'T'HH:mm:ss'Z'" ISO8601-compliant string to Date object.
     * Example of input: 2020-02-26T09:41:57Z
     *
     * @param dateTime string representation of datetime
     * @return Date object
     */
    public static CustomAttributeValue.DateTime dateTimeFromISO8601DateUTCString(String dateTime) throws ParseException {
        if (dateTime == null) {
            return null;
        }

        return new CustomAttributeValue.DateTime(dateFromISO8601DateUTCString(dateTime));
    }

    /**
     * Returns "yyyy-MM-dd" string for the supplied date.
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToYMDString(Date date) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(DATE_YMD_FORMAT, Locale.UK).format(date);
    }

    /**
     * Returns "yyyy-MM-dd" string for the supplied date in the current locale.
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToYMDStringLocale(Date date) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(DATE_YMD_FORMAT, Locale.getDefault()).format(date);
    }

    /**
     * Returns "yy-MM-dd-hh-mm-ss" string for the supplied date.
     *
     * @param date date object
     * @return String representation of Date object
     */
    public static String dateToYMDHMSString(Date date) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(DATE_YMD_HMS_FORMAT, Locale.getDefault()).format(date);
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

        Date nowDate = Time.date();
        return DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
    }

    public static String getGMTTimeZoneOffset() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        return new SimpleDateFormat("ZZZZ", Locale.getDefault()).format(calendar.getTime());
    }
}