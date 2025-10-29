/*
 * CustomAttributeMapperTest.java
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

public class CustomAttributeMapperTest {

    @Test
    public void test_customValueNull() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(null);

        assertNull(backendCustomValue);
    }

    @Test
    public void test_customPropertyBoolean() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(true));

        assertEquals(true, backendCustomValue);
    }

    @Test
    public void test_customValueString() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue("string"));

        assertEquals("string", backendCustomValue);
    }

    @Test
    public void test_customValueDate() throws Exception {
        Date someDate = new GregorianCalendar(2020, Calendar.MARCH, 31).getTime();
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(someDate));

        assertEquals("2020-03-31", backendCustomValue);
    }

    @Test
    public void test_customValueDateArab() throws Exception {
        String someString = "١٩٨٩-١١-٢١";
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(someString, CustomAttributeValue.Type.Date));

        assertEquals("1989-11-21", backendCustomValue);
    }

    @Test
    public void test_customValueDecimal() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(1.1));

        assertEquals(1.1, backendCustomValue);
    }

    @Test
    public void test_customValueLong() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(1L));

        assertEquals(1L, backendCustomValue);
    }

    @Test
    public void test_customValueInt() throws Exception {
        Object backendCustomValue = CustomAttributesMapper.customValueToBackend(new CustomAttributeValue(1));

        assertEquals(1, backendCustomValue);
    }

    @Test
    public void test_customPropertiesToBackend() throws Exception {
        Date someDate = new GregorianCalendar(2020, Calendar.MARCH, 31, 12, 0).getTime();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        CustomAttributeValue value1 = new CustomAttributeValue(1);
        CustomAttributeValue value2 = new CustomAttributeValue(2.2);
        CustomAttributeValue value3 = new CustomAttributeValue("2");
        CustomAttributeValue value4 = new CustomAttributeValue(false);
        CustomAttributeValue value5 = new CustomAttributeValue(someDate);
        customAttributes.put("value1", value1);
        customAttributes.put("value2", value2);
        customAttributes.put("value3", value3);
        customAttributes.put("value4", value4);
        customAttributes.put("value5", value5);

        Map backendCustomValue = CustomAttributesMapper.customAttsToBackend(customAttributes);

        assertEquals(5, backendCustomValue.size());
        assertEquals(value1.numberValue(), backendCustomValue.get("value1"));
        assertEquals(value2.numberValue(), backendCustomValue.get("value2"));
        assertEquals(value3.stringValue(), backendCustomValue.get("value3"));
        assertEquals(value4.booleanValue(), backendCustomValue.get("value4"));
        assertEquals(DateTimeUtil.dateToYMDString(value5.dateValue()), backendCustomValue.get("value5"));
    }
}
