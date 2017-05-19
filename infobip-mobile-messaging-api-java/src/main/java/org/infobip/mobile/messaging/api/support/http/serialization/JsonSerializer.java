package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class JsonSerializer {

    private Gson gson = new GsonBuilder().serializeNulls().create();

    public <T> T deserialize(String s, Class<T> type) {
        return gson.fromJson(s, type);
    }

    public <T> String serialize(T t) {
        return gson.toJson(t);
    }

    public String getStringPropertyFromJson(String json, String property) {
        try {
            return new JsonParser().parse(json).getAsJsonObject().get(property).getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
