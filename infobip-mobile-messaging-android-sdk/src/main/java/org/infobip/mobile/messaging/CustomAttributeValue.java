/*
 * CustomAttributeValue.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * This class wraps custom attribute types used to interact with backend services. The custom parameters may be of following types:
 * <ul>
 * <li>{@link String}</li>
 * <li>{@link Number}</li>
 * <li>{@link Date}</li>
 * <li>{@link Boolean}</li>
 * </ul>
 *
 * @see User#setCustomAttributes(Map)
 * @see User#setCustomAttribute(String, CustomAttributeValue)
 * @see User#getCustomAttributes()
 * @see User#getCustomAttributeValue(String)
 * @see Installation#setCustomAttributes(Map)
 * @see Installation#setCustomAttribute(String, CustomAttributeValue)
 * @see Installation#getCustomAttributes()
 * @see Installation#getCustomAttributeValue(String)
 */
public class CustomAttributeValue {

    public enum Type {
        String,
        Number,
        Date,
        Boolean,
        CustomList,
        DateTime
    }

    private Object value;
    private final Type type;

    public CustomAttributeValue(String someString) {
        this.value = someString;
        this.type = Type.String;
    }

    public CustomAttributeValue(Number someNumber) {
        this.value = someNumber;
        this.type = Type.Number;
    }

    public CustomAttributeValue(Date someDate) {
        this.value = DateTimeUtil.dateToISO8601String(someDate);
        this.type = Type.Date;
    }

    public CustomAttributeValue(DateTime someDateTime) {
        this.value = DateTimeUtil.dateTimeToISO8601UTCString(someDateTime);
        this.type = Type.DateTime;
    }

    public CustomAttributeValue(Boolean someBoolean) {
        this.value = someBoolean;
        this.type = Type.Boolean;
    }

    protected CustomAttributeValue(List<ListCustomAttributeItem> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ListCustomAttributeItem item: list) {
            result.add(item.getMap());
        }
        this.value = result;
        this.type = Type.CustomList;
    }

    /**
     * Parses string into CustomAttributeValue based on desired format.
     * <br>
     * For Date type this constructor accepts "yyyy-MM-dd" representation of date (for example 2016-12-31).
     *
     * @throws ParseException            if stringValue cannot be parsed to {@code CustomAttributeValue}
     * @throws InvalidParameterException if provided type is invalid
     */
    public CustomAttributeValue(String stringValue, Type type) throws ParseException, InvalidParameterException {
        this.type = type;
        switch (type) {
            case String:
                this.value = stringValue;
                break;
            case Number:
                this.value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(stringValue);
                break;
            case Date:
                DateTimeUtil.dateFromYMDString(stringValue);  // here for validation
                this.value = stringValue;
                break;
            case DateTime:
                DateTimeUtil.dateTimeFromISO8601DateUTCString(stringValue);  // here for validation
                this.value = stringValue;
                break;
            case Boolean:
                this.value = Boolean.valueOf(stringValue);
                break;
            case CustomList:
                throw new InvalidParameterException("You aren't able to create CustomAttributeValue with Type.CustomList");
            default:
                throw new InvalidParameterException();
        }
    }

    protected CustomAttributeValue(CustomAttributeValue that) {
        this.value = that.value;
        this.type = that.type;
    }

    /**
     * Return the value of specified {@code CustomAttributeValue} as {@link String}.
     *
     * @return {@link String}
     * @throws ClassCastException if {@code CustomAttributeValue} is not of {@link String} type.
     */
    public String stringValue() {
        if (!(value instanceof String) || type != Type.String) {
            throw new ClassCastException();
        }

        return (String) value;
    }

    /**
     * Return the value of specified {@code CustomAttributeValue} as {@link Number}.
     *
     * @return {@link Number}
     * @throws ClassCastException if {@code CustomAttributeValue} is not of {@link Number} type.
     */
    public Number numberValue() {
        if (!(value instanceof Number) || type != Type.Number) {
            throw new ClassCastException();
        }

        return (Number) value;
    }

    /**
     * Return the value of specified {@code CustomAttributeValue} as {@link Date}.
     *
     * @return {@link Date}
     * @throws ClassCastException if {@code CustomAttributeValue} is not of {@link Date} type.
     */
    public Date dateValue() {
        if (!(value instanceof String) || type != Type.Date) {
            throw new ClassCastException();
        }

        try {
            return DateTimeUtil.dateFromISO8601DateUTCString((String) value);
        } catch (ParseException ex1) {
            try {
                return DateTimeUtil.dateFromYMDString((String) value);
            } catch (ParseException ex2) {
                throw new ClassCastException(ex2.getMessage());
            }
        }
    }

    /**
     * Return the value of specified {@code CustomAttributeValue} as {@link DateTime}.
     *
     * @return {@link DateTime}
     * @throws ClassCastException if {@code CustomAttributeValue} is not of {@link DateTime} type.
     */
    public DateTime dateTimeValue() {
        if (!(value instanceof String) || type != Type.DateTime) {
            throw new ClassCastException();
        }

        try {
            return DateTimeUtil.dateTimeFromISO8601DateUTCString((String) value);
        } catch (ParseException ex1) {
            throw new ClassCastException(ex1.getMessage());
        }
    }

    /**
     * Return the value of specified {@code CustomAttributeValue} as {@link Boolean}.
     *
     * @return {@link Boolean}
     * @throws ClassCastException if {@code CustomAttributeValue} is not of {@link Boolean} type.
     */
    public Boolean booleanValue() {
        if (!(value instanceof Boolean) || type != Type.Boolean) {
            throw new ClassCastException();
        }

        return Boolean.valueOf("" + value);
    }

    protected List<Map<String, Object>> listMapValue() {
        if (!(value instanceof List) || type != Type.CustomList) {
            throw new ClassCastException();
        }
        return (List<Map<String, Object>>) value;
    }

    public Type getType() {
        return type;
    }

    protected Object getValue() {
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        if (this.type == null) {
            return super.toString();
        }

        switch (type) {
            case String:
                return stringValue();
            case Date:
                return DateTimeUtil.dateToYMDString(dateValue());
            case DateTime:
                return DateTimeUtil.dateTimeToISO8601UTCString(dateTimeValue());
            case Number:
                return "" + numberValue();
            case Boolean:
                return "" + booleanValue();
            default:
                return super.toString();
        }
    }

    public static class DateTime {
        private Date date;

        public DateTime(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @NonNull
        @Override
        public String toString() {
            return "DateTime{" +
                    "date=" + getDate() +
                    '}';
        }
    }

}
