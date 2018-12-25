package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class JsonSerializer {

    public interface ObjectAdapter<T> {
        Class<T> getCls();
        T deserialize(String value);
        String serialize(T value);
    }

    private class CustomTypeAdapter extends TypeAdapter<Object> {

        private final ObjectAdapter adapter;

        CustomTypeAdapter(ObjectAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            //noinspection unchecked
            out.jsonValue(adapter.serialize(value));
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            JsonElement element = new JsonParser().parse(in);
            return adapter.deserialize(element.toString());
        }
    }

    public JsonSerializer() {
        //TODO revert to serializeNulls?
        gson = new GsonBuilder().create();
    }

    public JsonSerializer(boolean serializeNulls, ObjectAdapter... adapters) {
        GsonBuilder builder = new GsonBuilder();
        if (serializeNulls) {
            builder = builder.serializeNulls();
        }
        if (adapters.length > 0) {
            for (ObjectAdapter adapter : adapters) {
                builder.registerTypeHierarchyAdapter(adapter.getCls(), new CustomTypeAdapter(adapter));
            }
        }
        gson = builder.create();
    }

    private final Gson gson;

    public <T> T deserialize(String s, Class<T> type) {
        return gson.fromJson(s, type);
    }

    public <T> T deserialize(String s, Type type) {
        return gson.fromJson(s, type);
    }

    public <T> String serialize(T t) {
        return gson.toJson(t);
    }
}
