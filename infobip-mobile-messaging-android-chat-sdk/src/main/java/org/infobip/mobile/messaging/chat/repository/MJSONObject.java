package org.infobip.mobile.messaging.chat.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class MJSONObject extends JSONObject {

    private MJSONObject() {
        super();
    }

    private MJSONObject(String json) throws JSONException {
        super(json);
    }

    private MJSONObject(JSONObject o, String... names) throws JSONException {
        super(o, names);
    }

    public static MJSONObject copy(JSONObject object, String... keysToExclude) {
        if (object == null) {
            return null;
        }

        try {
            return new MJSONObject(object, iteratorToArray(object.keys(), keysToExclude));
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot clone json object", e);
            return new MJSONObject();
        }
    }

    public static @Nullable MJSONObject create(String json) {
        if (json == null) {
            return null;
        }

        try {
            return new MJSONObject(json);
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot create json object from " + json, e);
            return null;
        }
    }

    public static @NonNull MJSONObject create() {
        return new MJSONObject();
    }

    public MJSONObject add(String key, Object value) {
        try {
            super.putOpt(key, value);
        } catch (JSONException e) {
            MobileMessagingLogger.e("Cannot put data to json object (" + key + ":" + value.toString() + ")", e);
        }
        return this;
    }

    // region private methods

    private static String[] iteratorToArray(Iterator<String> iterator, String... toExclude) {
        Set<String> exclude = arrayToSet(toExclude);
        List<String> values = new ArrayList<>();
        while (iterator.hasNext()) {
            String value = iterator.next();
            if (!exclude.contains(value)) {
                values.add(value);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    private static Set<String> arrayToSet(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    // endregion
}
