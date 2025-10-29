/*
 * EventPropertiesMapperTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventPropertiesMapperTest {

    @Test
    public void test_customPropertyNull() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(null);

        assertNull(backendCustomValue);
    }

    @Test
    public void test_customPropertyBoolean() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue(true));

        assertEquals(true, backendCustomValue);
    }

    @Test
    public void test_customPropertyString() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue("string"));

        assertEquals("string", backendCustomValue);
    }

    @Test
    public void test_customPropertyDate() throws Exception {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(2020, Calendar.MARCH, 31, 8, 0);
        Date someDate = gregorianCalendar.getTime();

        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue(someDate));

        assertEquals("2020-03-31T08:00:00Z", backendCustomValue);
    }

    @Test
    public void test_customPropertyDecimal() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue(1.1));

        assertEquals(1.1, backendCustomValue);
    }

    @Test
    public void test_customPropertyLong() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue(1L));

        assertEquals(1L, backendCustomValue);
    }

    @Test
    public void test_customPropertyInt() throws Exception {
        Object backendCustomValue = EventPropertiesMapper.eventPropertyToBackend(new CustomAttributeValue(1));

        assertEquals(1, backendCustomValue);
    }

    @Test
    public void test_customPropertiesToBackend() throws Exception {
        Date someDate = new GregorianCalendar(2020, Calendar.MARCH, 31).getTime();
        Map<String, CustomAttributeValue> eventProperties = new HashMap<>();
        CustomAttributeValue value1 = new CustomAttributeValue(1);
        CustomAttributeValue value2 = new CustomAttributeValue(2.2);
        CustomAttributeValue value3 = new CustomAttributeValue("2");
        CustomAttributeValue value4 = new CustomAttributeValue(false);
        CustomAttributeValue value5 = new CustomAttributeValue(someDate);
        eventProperties.put("value1", value1);
        eventProperties.put("value2", value2);
        eventProperties.put("value3", value3);
        eventProperties.put("value4", value4);
        eventProperties.put("value5", value5);

        Map backendCustomValue = EventPropertiesMapper.eventPropertiesToBackend(eventProperties);

        assertEquals(5, backendCustomValue.size());
        assertEquals(value1.numberValue(), backendCustomValue.get("value1"));
        assertEquals(value2.numberValue(), backendCustomValue.get("value2"));
        assertEquals(value3.stringValue(), backendCustomValue.get("value3"));
        assertEquals(value4.booleanValue(), backendCustomValue.get("value4"));
        assertEquals(DateTimeUtil.dateToISO8601UTCString(value5.dateValue()), backendCustomValue.get("value5"));
    }
}
