/*
 * JSONObjectAdapter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.json;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class JSONObjectAdapter implements JsonSerializer.ObjectAdapter<JSONObject> {
    @Override
    public Class<JSONObject> getCls() {
        return JSONObject.class;
    }

    @Override
    public JSONObject deserialize(String value) {
        if (value == null) {
            return null;
        }

        try {
            return new JSONObject(value);
        } catch (JSONException e) {
            MobileMessagingLogger.e("Error parsing JSONObject from " + value, e);
            return null;
        }
    }

    @Override
    public String serialize(JSONObject value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }
}
