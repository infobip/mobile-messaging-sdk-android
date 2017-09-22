package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.ISO8601DateParseException;

import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.text.ParseException;
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

    public enum Type {
        String,
        Number,
        Date
    }

    private Object value;
    private final Type type;

    public CustomUserDataValue(String someString) {
        this.value = someString;
        this.type = Type.String;
    }

    public CustomUserDataValue(Number someNumber) {
        this.value = someNumber;
        this.type = Type.Number;
    }

    public CustomUserDataValue(Date someDate) {
        this.value = DateTimeUtil.ISO8601DateToString(someDate);
        this.type = Type.Date;
    }

    /**
     * Parses string into CustomUserDataValue based on desired format.
     * </p>
     * For Date type this constructor accepts "yyyy-MM-dd" representation of date (for example 2016-12-31).
     *
     * @throws ParseException            if stringValue cannot be parsed to {@code CustomUserDataValue}
     * @throws InvalidParameterException if provided type is invalid
     */
    public CustomUserDataValue(String stringValue, Type type) throws ParseException, InvalidParameterException {
        this.type = type;
        switch (type) {
            case String:
                this.value = stringValue;
                break;
            case Number:
                this.value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(stringValue);
                break;
            case Date:
                DateTimeUtil.DateFromYMDString(stringValue);
                this.value = stringValue;
                break;
            default:
                throw new InvalidParameterException();
        }
    }

    protected CustomUserDataValue(CustomUserDataValue that) {
        this.value = that.value;
        this.type = that.type;
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link String}.
     *
     * @return {@link String}
     * @throws ClassCastException if {@code CustomUserDataValue} is not of {@link String} type.
     */
    public String stringValue() {
        if (!(value instanceof String) || type != Type.String) {
            throw new ClassCastException();
        }

        return (String) value;
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link Number}.
     *
     * @return {@link Number}
     * @throws ClassCastException if {@code CustomUserDataValue} is not of {@link Number} type.
     */
    public Number numberValue() {
        if (!(value instanceof Number) || type != Type.Number) {
            throw new ClassCastException();
        }

        return (Number) value;
    }

    /**
     * Return the value of specified {@code CustomUserDataValue} as {@link Date}.
     *
     * @return {@link Date}
     * @throws ClassCastException if {@code CustomUserDataValue} is not of {@link Date} type.
     */
    public Date dateValue() {
        if (!(value instanceof String) || type != Type.Date) {
            throw new ClassCastException();
        }

        try {
            return DateTimeUtil.ISO8601DateFromString((String) value);
        } catch (ISO8601DateParseException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    public Type getType() {
        return type;
    }

    protected Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (this.type == null) {
            return super.toString();
        }

        switch (type) {
            case String:
                return stringValue();
            case Date:
                return DateTimeUtil.DateToYMDString(dateValue());
            case Number:
                return "" + numberValue();
            default:
                return super.toString();
        }
    }
}
