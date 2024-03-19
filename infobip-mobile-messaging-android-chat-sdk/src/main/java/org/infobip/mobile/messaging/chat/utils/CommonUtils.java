package org.infobip.mobile.messaging.chat.utils;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

public class CommonUtils {
    private CommonUtils() {
    }

    public static String escapeJsonString(String source) {
        String serialize = new JsonSerializer().serialize(source);
        return serialize.substring(1, serialize.length() - 1);
    }
}
