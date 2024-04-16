package org.infobip.mobile.messaging.chat.utils;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.json.JSONObject;

public class CommonUtils {
    private CommonUtils() {
    }

    public static String escapeJsonString(String source) {
        String serialize = new JsonSerializer().serialize(source);
        return serialize.substring(1, serialize.length() - 1);
    }

    public static Boolean isJSON(String message) {
        try {
            new JSONObject(message);
        } catch (Throwable throwable) {
            return false;
        }
        return true;
    }

}
