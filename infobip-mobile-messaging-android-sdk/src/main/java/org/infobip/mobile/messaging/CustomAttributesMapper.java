package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
            case Number:
                return value.numberValue();
            case String:
                return value.stringValue();
            case Boolean:
                return value.booleanValue();
            default:
                return null;
        }
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

            if (value instanceof String) {
                String stringValue = (String) value;

                if (isPossiblyDate(stringValue)) {
                    try {
                        Date dateValue = DateTimeUtil.dateFromYMDString(stringValue);
                        customUserDataValueMap.put(key, new CustomAttributeValue(dateValue));
                        continue;
                    } catch (ParseException ignored) {
                    }
                }
                customUserDataValueMap.put(key, new CustomAttributeValue(stringValue));

            } else if (value instanceof Number) {
                customUserDataValueMap.put(key, new CustomAttributeValue((Number) value));
            } else if (value instanceof Boolean) {
                customUserDataValueMap.put(key, new CustomAttributeValue((Boolean) value));
            }
        }

        return customUserDataValueMap;
    }

    private static boolean isPossiblyDate(String stringValue) {
        return Character.isDigit(stringValue.charAt(0)) && stringValue.length() == DateTimeUtil.DATE_YMD_FORMAT.length();
    }

}
