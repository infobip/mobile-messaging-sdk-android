package org.infobip.mobile.messaging.api.support;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class MapModel {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    private Map<String, Object> map = new HashMap<>();

    public Map<String, Object> getMap() {
        return map;
    }

    public <T> T getField(String name) {
        //noinspection unchecked
        return (T) map.get(name);
    }

    public <T> void setField(String name, T value) {
        map.put(name, value);
    }

    public boolean containsField(String name) {
        return map.containsKey(name);
    }

    public boolean hasDataToReport() {
        return !map.isEmpty();
    }

    public void clearUnreportedData() {
        map.clear();
    }

    @Override
    public String toString() {
        return nullSerializer.serialize(map);
    }
}