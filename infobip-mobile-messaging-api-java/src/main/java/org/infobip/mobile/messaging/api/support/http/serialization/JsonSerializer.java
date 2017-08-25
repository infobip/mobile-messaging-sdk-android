package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class JsonSerializer {

    public JsonSerializer() {
        gson = new GsonBuilder().serializeNulls().create();
    }

    public JsonSerializer(boolean serializeNulls) {
        if (serializeNulls) {
            gson = new GsonBuilder().serializeNulls().create();
        } else {
            gson = new Gson();
        }
    }

    private final Gson gson;

    public <T> T deserialize(String s, Class<T> type) {
        return gson.fromJson(s, type);
    }

    public <T> String serialize(T t) {
        return gson.toJson(t);
    }
}
