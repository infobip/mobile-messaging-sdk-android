package org.infobip.mobile.messaging.util;

import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonElement;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonObject;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonParser;

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

    private static String getSilentDataField(Message message, String silentKey) {
        Bundle bundle = message.getBundle();
        String internalDataString = bundle.getString(Data.INTERNAL_DATA.getKey());
        if (internalDataString == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject internalData = jsonParser.parse(internalDataString).getAsJsonObject();
        if (internalData == null) {
            return null;
        }

        JsonObject silentData = internalData.getAsJsonObject(InternalData.SILENT_DATA.getKey());
        if (silentData == null) {
            return null;
        }

        JsonElement element = silentData.get(silentKey);
        if (element == null) {
            return null;
        }

        return element.getAsString();
    }

    private static void setSilentDataField(Message message, String silentKey, String value) {
        Bundle bundle = message.getBundle();
        String internalDataString = bundle.getString(Data.INTERNAL_DATA.getKey());
        JsonParser jsonParser = new JsonParser();
        JsonObject internalData = internalDataString == null ? new JsonObject() : jsonParser.parse(internalDataString).getAsJsonObject();;
        if (internalData == null) {
            internalData = new JsonObject();

        }

        JsonObject silentData = internalData.getAsJsonObject(InternalData.SILENT_DATA.getKey());
        if (silentData == null) {
            silentData = new JsonObject();
        }

        silentData.addProperty(silentKey, value);
        internalData.add(InternalData.SILENT_DATA.getKey(), silentData);
        bundle.putString(Data.INTERNAL_DATA.getKey(), internalData.toString());
    }

    private enum InternalData {
        SILENT_DATA("silent"),
        TITLE("title"),
        BODY("body"),
        SOUND("sound");

        private final String key;

        InternalData(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
