/*
 * MobileInboxMappersTest.java
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
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.infobip.mobile.messaging.inbox.MobileInboxFilterOptionsJson.mobileInboxFilterOptionsFromJSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MobileInboxMappersTest {

    @Test
    public void inboxMapper_toJSON_return_empty_for_null() {
        JSONObject inbox = InboxMapper.toJSON(null);
        assertEquals(inbox.length(), 0);
    }

    @Test
    public void inboxMapper_toJSON() {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(2);
        inbox.setCountUnread(1);
        JSONObject inboxJSON = InboxMapper.toJSON(inbox);
        assertEquals(inboxJSON.toString(), "{\"countTotal\":2,\"countUnread\":1}");
    }

    @Test
    public void mobileInboxFilterOptionsFromJSON_is_null_for_null() {
        assertNull(mobileInboxFilterOptionsFromJSON(null));
    }

    @Test
    public void mobileInboxFilterOptionsFromJSON_is_object_with_nulls() throws JSONException {
        MobileInboxFilterOptions filterOptions = mobileInboxFilterOptionsFromJSON(new JSONObject("{}"));

        assertNull(filterOptions.getLimit());
        assertNull(filterOptions.getTopic());
        assertNull(filterOptions.getFromDateTime());
        assertNull(filterOptions.getToDateTime());
    }

    @Test
    public void mobileInboxFilterOptionsFromJSON_is_object_with_actual_values() throws JSONException {
        String fromDateStr = "2024-11-07T00:00:00Z";
        String toDateStr = "2024-11-08T00:00:00Z";
        int limit = 100;
        String topic = "topicName";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromDateTime", fromDateStr);
        jsonObject.put("toDateTime", toDateStr);
        jsonObject.put("topic", topic);
        jsonObject.put("limit", limit);

        MobileInboxFilterOptions filterOptions = mobileInboxFilterOptionsFromJSON(jsonObject);

        assertEquals(limit, (int) filterOptions.getLimit());
        assertEquals("topicName", filterOptions.getTopic());
        assertEquals(DateTimeUtil.ISO8601DateFromString(fromDateStr), filterOptions.getFromDateTime());
        assertEquals(DateTimeUtil.ISO8601DateFromString(toDateStr), filterOptions.getToDateTime());
    }

    @Test
    public void mobileInboxFilterOptionsFromJSON_is_object_with_topics_define() throws JSONException {
        String fromDateStr = "2024-11-07T00:00:00Z";
        String toDateStr = "2024-11-08T00:00:00Z";
        int limit = 100;
        List<String> topics = Arrays.asList("topic1", "topic2");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromDateTime", fromDateStr);
        jsonObject.put("toDateTime", toDateStr);
        jsonObject.put("topics", new JSONArray(topics));
        jsonObject.put("limit", limit);

        MobileInboxFilterOptions filterOptions = mobileInboxFilterOptionsFromJSON(jsonObject);

        assertEquals(limit, (int) filterOptions.getLimit());
        assertEquals(topics, filterOptions.getTopics());
        assertEquals(DateTimeUtil.ISO8601DateFromString(fromDateStr), filterOptions.getFromDateTime());
        assertEquals(DateTimeUtil.ISO8601DateFromString(toDateStr), filterOptions.getToDateTime());
    }
}
