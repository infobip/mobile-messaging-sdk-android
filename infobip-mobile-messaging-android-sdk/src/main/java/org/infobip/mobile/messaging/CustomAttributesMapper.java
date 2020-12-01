package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomAttributesMapper {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    public static Map<String, Object> customAttsToBackend(@NonNull Map<String, CustomAttributeValue> customAttributes) {
        Map<String, Object> customAttributesToReport = new HashMap<>(customAttributes.size());
        for (Map.Entry<String, CustomAttributeValue> entry : customAttributes.entrySet()) {
            customAttributesToReport.put(entry.getKey(), customValueToBackend(entry.getValue()));
        }
        return customAttributesToReport;
    }

    public static Object customValueToBackend(CustomAttributeValue value) {
        if (value == null) {
            return null;
        }

        switch (value.getType()) {
            case Date:
                return DateTimeUtil.dateToYMDString(value.dateValue());
            case DateTime:
                return DateTimeUtil.dateTimeToISO8601UTCString(value.dateTimeValue());
            case Number:
                return value.numberValue();
            case String:
                return value.stringValue();
            case Boolean:
                return value.booleanValue();
            case CustomList:
                return value.listMapValue();
            default:
                return null;
        }
    }

    public static Boolean validate(Map<String, Object> customAttributes) {
        for (Map.Entry<String, Object> entry: customAttributes.entrySet()) {
            if (!(entry.getValue() instanceof List)) continue;

            List valueList = (List) entry.getValue();
            Set<String> keys = new HashSet<>();
            Map<String, CustomAttributeValue> valuesMap = new HashMap<>();
            for (Object valueItem: valueList) {
                if (!(valueItem instanceof Map)) {
                    return false;
                }
                Map<String, Object> map = (Map<String, Object>) valueItem;
                if (map.keySet().isEmpty()) {
                    return false;
                }
                if (keys.isEmpty()) {
                    keys.addAll(map.keySet());
                    for (Map.Entry<String, Object> listEntry: map.entrySet()) {
                        valuesMap.put(listEntry.getKey(), parseValue(listEntry.getValue()));
                    }
                    continue;
                }
                if (keys.size() != map.keySet().size()) {
                    return false;
                }
                if (!keys.containsAll(map.keySet()) || !map.keySet().containsAll(keys)) {
                    return false;
                }

                for (Map.Entry<String, Object> listEntry: map.entrySet()) {
                    CustomAttributeValue value = parseValue(listEntry.getValue());
                    CustomAttributeValue existedValue = valuesMap.get(listEntry.getKey());
                    if (!existedValue.getType().equals(value.getType())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Map<String, CustomAttributeValue> customAttsFrom(String json) {
        Type type = new TypeToken<Map<String, CustomAttributeValue>>() {
        }.getType();
        return nullSerializer.deserialize(json, type);
    }

    public static Map<String, CustomAttributeValue> customAttsFromBackend(Map<String, Object> customAttributes) {
        Map<String, CustomAttributeValue> customUserDataValueMap = new HashMap<>();
        if (customAttributes == null) {
            return customUserDataValueMap;
        }

        for (Map.Entry<String, Object> entry : customAttributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                List valueList = (List) value;
                List<ListCustomAttributeItem> customAttributeItems = new LinkedList<>();
                for (Object valueItem: valueList) {
                    if (!(valueItem instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> map = (Map<String, Object>) valueItem;
                    ListCustomAttributeItem.Builder attributeItem = ListCustomAttributeItem.builder();
                    for (Map.Entry<String, Object> mapEntry: map.entrySet()) {
                        CustomAttributeValue customAttribute = parseValue(mapEntry.getValue());
                        switch (customAttribute.getType()) {
                            case String: attributeItem.putString(mapEntry.getKey(), customAttribute.stringValue()); break;
                            case Date: attributeItem.putDate(mapEntry.getKey(), customAttribute.dateValue()); break;
                            case DateTime: attributeItem.putDateTime(mapEntry.getKey(), customAttribute.dateTimeValue()); break;
                            case Number: attributeItem.putNumber(mapEntry.getKey(), customAttribute.numberValue()); break;
                            case Boolean: attributeItem.putBoolean(mapEntry.getKey(), customAttribute.booleanValue()); break;
                        }
                    }
                    customAttributeItems.add(attributeItem.build());
                }
                customUserDataValueMap.put(key, new CustomAttributeValue(customAttributeItems));
            } else {
                customUserDataValueMap.put(key, parseValue(value));
            }
        }

        return customUserDataValueMap;
    }

    private static CustomAttributeValue parseValue(Object value) {
        if (value instanceof String) {
            String stringValue = (String) value;

            if (isPossiblyDateOrDateTime(stringValue)) {
                Date dateValue;
                try {
                    dateValue = DateTimeUtil.dateFromISO8601DateUTCString((String) value);
                    return new CustomAttributeValue(new CustomAttributeValue.DateTime(dateValue));
                } catch (ParseException ex1) {
                    try {
                        dateValue = DateTimeUtil.dateFromYMDString((String) value);
                        return new CustomAttributeValue(dateValue);
                    } catch (ParseException ignored) {
                    }
                }
            }
            return new CustomAttributeValue(stringValue);
        } else if (value instanceof Number) {
            return new CustomAttributeValue((Number) value);
        } else {
            return new CustomAttributeValue((Boolean) value);
        }
    }

    private static boolean isPossiblyDateOrDateTime(String stringValue) {
        return Character.isDigit(stringValue.charAt(0)) &&
                (stringValue.length() == DateTimeUtil.DATE_YMD_FORMAT.length() ||
                        stringValue.length() == DateTimeUtil.DATE_TIME_LENGTH_DATE_FORMAT3);
    }

}
