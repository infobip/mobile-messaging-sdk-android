package org.infobip.mobile.messaging.chat.utils;

import android.os.Build;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

public class CommonUtils {
    private CommonUtils() {
    }

    public static boolean isOSOlderThanKitkat() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
    }

    public static String escapeJsonString(String source) {
        String serialize = new JsonSerializer(false).serialize(source);
        return serialize.substring(1, serialize.length() - 1);
    }
}
