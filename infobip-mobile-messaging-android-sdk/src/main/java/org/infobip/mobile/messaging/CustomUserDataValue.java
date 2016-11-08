package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.util.ISO8601DateParseException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * This class wraps user data types used to interact with backend services. The custom parameters may be of following types:
 * <ul>
 * <li>{@link String}</li>
 * <li>{@link Number}</li>
 * <li>{@link Date}</li>
 * </ul>
 *
 * @see UserData#setCustomUserData(Map)
 * @see UserData#setCustomUserDataElement(String, CustomUserDataValue)
 * @see UserData#getCustomUserData()
 * @see UserData#getCustomUserDataValue(String)
 */
public class CustomUserDataValue {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String GMT_TIME_ZONE = "+00:00";
    private static final String ISO8601_GMT_Z_MATCHER = "Z$";

    private Object value;
    private String type;

    public CustomUserDataValue() {
    }

    public CustomUserDataValue(String someString) {
        set(someString);
    }

    public CustomUserDataValue(Number someNumber) {
        set(someNumber);
    }

    public CustomUserDataValue(Date someDate) {
        set(someDate);
    }

    public void set(String someString) {
        this.value = someString;
        this.type = getTypeForValue(someString);
    }

    public void set(Number someNumber) {
        this.value = someNumber;
        this.type = getTypeForValue(someNumber);
    }

    public void set(Date someDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        this.value = simpleDateFormat.format(someDate);
        this.type = getTypeForValue(someDate);
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link String}.
     *
     * @return {@link String}
     * @throws ClassCastException if {@code CustomUserDataValue} is not type of {@link String}
     */
    public String stringValue() {
        if (!(value instanceof String)) {
            throw new ClassCastException();
        }

        return (String) value;
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link Number}.
     *
     * @return {@link Number}
     * @throws ClassCastException if {@code CustomUserDataValue} is not type of {@link Number}
     */
    public Number numberValue() {
        if (!(value instanceof Number)) {
            throw new ClassCastException();
        }

        return (Number) value;
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link Date}.
     *
     * @return {@link Date}
     * @throws ISO8601DateParseException if {@code CustomUserDataValue} is not type of {@link Date}.
     */
    public Date dateValue() {
        String dateValue = (String) this.value;
        String date = dateValue.trim().replaceAll(ISO8601_GMT_Z_MATCHER, GMT_TIME_ZONE);
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(date);
        } catch (ParseException e) {
            throw new ISO8601DateParseException(ISO8601DateParseException.Reason.DATE_PARSE_EXCEPTION, e);
        }
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    private String getTypeForValue(Object value) {
        if (value instanceof String) {
            return "String";
        }

        if (value instanceof Date) {
            return "Date";
        }

        if (value instanceof Number) {
            return "Number";
        }

        return null;
    }
}
