package org.infobip.mobile.messaging.mobile.data;

import org.infobip.mobile.messaging.CustomUserDataValue;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.data.CustomUserDataValueReport;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pandric
 * @since sdk v1.3.14
 */

class UserDataMapper extends UserData {

    private static class CustomValueMapper extends CustomUserDataValue {

        CustomValueMapper(CustomUserDataValue value) {
            super(value);
        }

        Object objectValue() {
            return getValue();
        }
    }

    private UserDataMapper(String externalUserId, Map<String, Object> predefinedUserData, Map<String, CustomUserDataValue> customUserData) {
        super(externalUserId, predefinedUserData, customUserData);
    }

    static UserData fromUserDataReport(String externalUserId, Map<String, Object> predefinedUserData, Map<String, CustomUserDataValueReport> customUserDataReport) {
        return new UserDataMapper(externalUserId, predefinedUserData, mapFromCustomUserDataReport(customUserDataReport));
    }

    static UserDataReport toUserDataReport(Map<String, Object> predefinedUserData, Map<String, CustomUserDataValue> customUserData) {
        return new UserDataReport(predefinedUserData, mapToCustomUserDataReport(customUserData));
    }

    private static Map<String, CustomUserDataValue> mapFromCustomUserDataReport(Map<String, CustomUserDataValueReport> customUserDataReportMap) {
        Map<String, CustomUserDataValue> customUserDataValueMap = new HashMap<>();
        if (customUserDataReportMap == null) {
            return customUserDataValueMap;
        }

        for (String key : customUserDataReportMap.keySet()) {
            CustomUserDataValueReport customUserDataValueReport = customUserDataReportMap.get(key);
            String type = customUserDataValueReport.getType();

            switch (type) {
                case "String":
                    customUserDataValueMap.put(key, new CustomUserDataValue((String) customUserDataValueReport.getValue()));
                    break;
                case "Number":
                    customUserDataValueMap.put(key, new CustomUserDataValue((Number) customUserDataValueReport.getValue()));
                    break;
                case "Date":
                    customUserDataValueMap.put(key, new CustomUserDataValue(DateTimeUtil.ISO8601DateFromString((String) customUserDataValueReport.getValue())));
                    break;
            }
        }

        return customUserDataValueMap;
    }

    private static Map<String, CustomUserDataValueReport> mapToCustomUserDataReport(Map<String, CustomUserDataValue> customUserDataValueMap) {
        Map<String, CustomUserDataValueReport> customUserDataValueReportMap = new HashMap<>(customUserDataValueMap.size());
        for (String key : customUserDataValueMap.keySet()) {
            CustomUserDataValue customUserDataValue = customUserDataValueMap.get(key);
            CustomUserDataValueReport customUserDataValueReport = customUserDataValue != null
                    ? new CustomUserDataValueReport(new CustomValueMapper(customUserDataValue).objectValue(), customUserDataValue.getType().name())
                    : null;
            customUserDataValueReportMap.put(key, customUserDataValueReport);
        }

        return customUserDataValueReportMap;
    }
}
