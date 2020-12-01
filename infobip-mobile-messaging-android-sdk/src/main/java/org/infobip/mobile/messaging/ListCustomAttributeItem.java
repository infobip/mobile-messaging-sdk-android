package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ListCustomAttributeItem {

    private Map<String, Object> customMap = new HashMap<>();

    private ListCustomAttributeItem() {}

    public void putString(String key, String value) {
        customMap.put(key, value);
    }

    public String getStringValue(String key) {
        return (String) customMap.get(key);
    }

    public void putDateTime(String key, CustomAttributeValue.DateTime value) {
        customMap.put(key, DateTimeUtil.dateTimeToISO8601UTCString(value));
    }

    public void putDate(String key, Date value) {
        customMap.put(key, DateTimeUtil.dateToYMDString(value));
    }

    public Date getDateValue(String key) {
        Object value = customMap.get(key);
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof CustomAttributeValue.DateTime) {
            return ((CustomAttributeValue.DateTime) value).getDate();
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

    public void putNumber(String key, Number value) {
        customMap.put(key, value);
    }

    public Number getNumberValue(String key) {
        return (Number) customMap.get(key);
    }

    public void putBoolean(String key, Boolean value) {
        customMap.put(key, value);
    }

    public Boolean getBooleanValue(String key) {
        return (Boolean) customMap.get(key);
    }

    public Map<String, Object> getMap() {
        return customMap;
    }

    public static Builder builder() {
        return new ListCustomAttributeItem(). new Builder();
    }

    public class Builder {

        private Builder() {}

        public Builder putString(String key, String value) {
            ListCustomAttributeItem.this.putString(key, value);
            return this;
        }

        public Builder putDateTime(String key, CustomAttributeValue.DateTime value) {
            ListCustomAttributeItem.this.putDateTime(key, value);
            return this;
        }

        public Builder putDate(String key, Date value) {
            ListCustomAttributeItem.this.putDate(key, value);
            return this;
        }

        public Builder putNumber(String key, Number value) {
            ListCustomAttributeItem.this.putNumber(key, value);
            return this;
        }

        public Builder putBoolean(String key, Boolean value) {
            ListCustomAttributeItem.this.putBoolean(key, value);
            return this;
        }

        public ListCustomAttributeItem build() {
            return ListCustomAttributeItem.this;
        }

    }
}
