/*
 * JSONArrayAdapter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.json;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class JSONArrayAdapter implements JsonSerializer.ObjectAdapter<JSONArray> {

    @Override
    public Class<JSONArray> getCls() {
        return JSONArray.class;
    }

    @Override
    public JSONArray deserialize(String value) {
        if (value == null) {
            return null;
        }

        try {
            return new JSONArray(value);
        } catch (JSONException e) {
            MobileMessagingLogger.e("Error parsing JSONArray from " + value, e);
            return null;
        }
    }

    @Override
    public String serialize(JSONArray value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }
}
