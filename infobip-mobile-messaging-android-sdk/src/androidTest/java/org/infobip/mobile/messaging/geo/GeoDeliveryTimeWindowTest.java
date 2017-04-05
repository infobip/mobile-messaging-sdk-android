package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author pandric
 * @since 06/10/16.
 */

public class GeoDeliveryTimeWindowTest extends MobileMessagingTestCase {

    @Test
    public void test_iso8601_time_interval_window() throws ParseException {
        String hoursISO8601 = "0815/1633";
        String[] timeIntervalStartEnd = hoursISO8601.split("/");
        String startTime = timeIntervalStartEnd[0];
        String endTime = timeIntervalStartEnd[1];

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HHmm", Locale.getDefault());

        Date startDate = simpleDateFormat.parse(startTime);
        Calendar timeStart = Calendar.getInstance();
        timeStart.setTime(startDate);

        Date endDate = simpleDateFormat.parse(endTime);
        Calendar timeEnd = Calendar.getInstance();
        timeEnd.setTime(endDate);

        // Delivery time window is between 08:15 and 16:33
        // delivery time inside window - 12:00
        Date nowDate = simpleDateFormat.parse("1200");
        boolean isDeliveryTimeInWindow = DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
        assertTrue(isDeliveryTimeInWindow);

        // delivery time outside window - 17:00
        nowDate = simpleDateFormat.parse("1700");
        isDeliveryTimeInWindow = DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
        assertFalse(isDeliveryTimeInWindow);

        // delivery time outside window - 08:15
        nowDate = simpleDateFormat.parse("0815");
        isDeliveryTimeInWindow = DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
        assertFalse(isDeliveryTimeInWindow);

        // delivery time outside window - 16:33
        nowDate = simpleDateFormat.parse("1633");
        isDeliveryTimeInWindow = DateTimeUtil.compareTimes(startDate, nowDate) < 0 && DateTimeUtil.compareTimes(nowDate, endDate) < 0;
        assertFalse(isDeliveryTimeInWindow);
    }

    @Test
    public void test_ISO8601_days_of_week() {
        String daysISO8601 = "1,2,3,4,5,6,7";
        String[] days = daysISO8601.split(",");

        boolean isTodayIncluded = false;
        for (String day : days) {
            int dayOfMonthISO8601 = DateTimeUtil.dayOfWeekISO8601();
            if (day.equalsIgnoreCase(String.valueOf(dayOfMonthISO8601))) {
                isTodayIncluded = true;
            }
        }

        assertTrue(isTodayIncluded);
    }
}
