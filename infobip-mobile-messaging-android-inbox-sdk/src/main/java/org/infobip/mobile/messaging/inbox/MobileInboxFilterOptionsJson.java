/*
 * MobileInboxFilterOptionsJson.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MobileInboxFilterOptionsJson {
    public static MobileInboxFilterOptions mobileInboxFilterOptionsFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        try {
            Date fromDateTime = null, toDateTime = null;
            String topic = null;
            List<String> topics = null;
            Integer limit = null;

            if (json.has(MobileInboxFilterOptionsAttrs.fromDateTime) && !json.isNull(MobileInboxFilterOptionsAttrs.fromDateTime))
                fromDateTime = DateTimeUtil.ISO8601DateFromString(json.getString(MobileInboxFilterOptionsAttrs.fromDateTime));
            if (json.has(MobileInboxFilterOptionsAttrs.toDateTime) && !json.isNull(MobileInboxFilterOptionsAttrs.toDateTime))
                toDateTime = DateTimeUtil.ISO8601DateFromString(json.getString(MobileInboxFilterOptionsAttrs.toDateTime));
            if (json.has(MobileInboxFilterOptionsAttrs.topic) && !json.isNull(MobileInboxFilterOptionsAttrs.topic))
                topic = json.getString(MobileInboxFilterOptionsAttrs.topic);
            if (json.has(MobileInboxFilterOptionsAttrs.topics) && !json.isNull(MobileInboxFilterOptionsAttrs.topics)) {
                JSONArray jaTopics = json.getJSONArray(MobileInboxFilterOptionsAttrs.topics);
                topics = new ArrayList<>(jaTopics.length());
                for (int i = 0; i < jaTopics.length(); i++) {
                    topics.add(jaTopics.getString(i));
                }
            }
            if (json.has(MobileInboxFilterOptionsAttrs.limit) && !json.isNull(MobileInboxFilterOptionsAttrs.limit))
                limit = json.getInt(MobileInboxFilterOptionsAttrs.limit);

            if (topic != null) {
                return new MobileInboxFilterOptions(
                        fromDateTime,
                        toDateTime,
                        topic,
                        limit
                );
            } else {
                return new MobileInboxFilterOptions(
                        fromDateTime,
                        toDateTime,
                        topics,
                        limit
                );
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}

