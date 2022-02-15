package org.infobip.mobile.messaging;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.util.HashMap;
import java.util.Map;

public class EventPropertiesMapper {

    @NonNull
    public static Map<String, Object> eventPropertiesToBackend(@NonNull Map<String, CustomAttributeValue> eventProperties) {
        Map<String, Object> eventPropertiesToReport = new HashMap<>(eventProperties.size());
        for (Map.Entry<String, CustomAttributeValue> entry : eventProperties.entrySet()) {
            eventPropertiesToReport.put(entry.getKey(), eventPropertyToBackend(entry.getValue()));
        }
        return eventPropertiesToReport;
    }

    static Object eventPropertyToBackend(CustomAttributeValue value) {
        if (value == null) {
            return null;
        }

        switch (value.getType()) {
            case Date:
                return DateTimeUtil.dateToISO8601UTCString(value.dateValue());
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
}
