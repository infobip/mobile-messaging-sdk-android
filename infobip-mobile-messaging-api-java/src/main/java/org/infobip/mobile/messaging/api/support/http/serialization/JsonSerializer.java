package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.Gson;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class JsonSerializer {

    private Gson gson = new Gson();

    public <T> T deserialize(String s, Class<T> type) {
        return gson.fromJson(s, type);
    }

    public <T> String serialize(T t) {
        return gson.toJson(t);
    }
}
