package org.infobip.mobile.messaging.util;

import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 27/06/16.
 */
public class InternalMessageUtils extends Message {

    public static String getSilentTitle(Message message) {
        return getSilentDataField(message, InternalData.TITLE.getKey());
    }

    public static void setSilentTitle(Message message, String title) {
        setSilentDataField(message, InternalData.TITLE.getKey(), title);
    }

    public static String getSilentBody(Message message) {
        return getSilentDataField(message, InternalData.BODY.getKey());
    }

    public static void setSilentBody(Message message, String body) {
        setSilentDataField(message, InternalData.BODY.getKey(), body);
    }

    public static String getSilentSound(Message message) {
        return getSilentDataField(message, InternalData.SOUND.getKey());
    }

    public static void setSilentSound(Message message, String sound) {
        setSilentDataField(message, InternalData.SOUND.getKey(), sound);
    }

    public static String getSilentCategory(Message message) {
        return getSilentDataField(message, InternalData.CATEGORY.getKey());
    }

    public static void setSilentCategory(Message message, String category) {
        setSilentDataField(message, InternalData.CATEGORY.getKey(), category);
    }

    private static String getSilentDataField(Message message, String silentKey) {
        String internalDataString = message.getBundle().getString(Data.INTERNAL_DATA.getKey());
        if (internalDataString == null) {
            return null;
        }

        JSONObject internalData;
        try {
            internalData = new JSONObject(internalDataString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject silentData = internalData.optJSONObject(InternalData.SILENT_DATA.getKey());
        if (silentData == null) {
            return null;
        }

        return silentData.optString(silentKey);
    }

    private static void setSilentDataField(Message message, String silentKey, String value) {
        Bundle bundle = message.getBundle();
        String internalDataString = message.getBundle().getString(Data.INTERNAL_DATA.getKey());
        JSONObject internalData = null;
        if (internalDataString != null) {
            try {
                internalData = new JSONObject(internalDataString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (internalData == null) {
            internalData = new JSONObject();
        }

        JSONObject silentData = internalData.optJSONObject(InternalData.SILENT_DATA.getKey());
        if (silentData == null) {
            silentData = new JSONObject();
        }

        try {
            silentData.put(silentKey, value);
            internalData.put(InternalData.SILENT_DATA.getKey(), silentData);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        bundle.putString(Data.INTERNAL_DATA.getKey(), internalData.toString());
    }

    private enum InternalData {
        SILENT_DATA("silent"),
        TITLE("title"),
        BODY("body"),
        SOUND("sound"),
        CATEGORY("category");

        private final String key;

        InternalData(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
