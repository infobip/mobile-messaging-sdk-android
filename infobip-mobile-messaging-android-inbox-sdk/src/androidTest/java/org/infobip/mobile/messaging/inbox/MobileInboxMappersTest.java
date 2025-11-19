/*
 * MobileInboxMappersTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import static org.infobip.mobile.messaging.inbox.MobileInboxFilterOptionsJson.mobileInboxFilterOptionsFromJSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MobileInboxMappersTest {

    @Test
    public void inboxMapper_toJSON_return_empty_for_null() {
        JSONObject inbox = InboxMapper.toJSON(null);
        assertEquals(0, inbox.length());
    }

    @Test
    public void inboxMapper_toJSON() throws JSONException {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(2);
        inbox.setCountUnread(1);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("custom", "value");
        List<InboxMessage> messages = new ArrayList<>();
        messages.add(new InboxMessage("123", "title", "message", "kittens", false, null,
                true, null, null, 1, 1, 1, jsonObject, null, null,
                null, null, null, "topic", false, null, 1, null, null, null, null, null, null));
        inbox.setMessages(messages);
        JSONObject inboxJSON = InboxMapper.toJSON(inbox);

        assertEquals("{\"countTotal\":2,\"countUnread\":1,\"messages\":[{\"messageId\":\"123\",\"title\":\"title\",\"body\":\"message\",\"sound\":\"kittens\",\"vibrate\":false,\"silent\":true,\"receivedTimestamp\":1,\"customPayload\":{\"custom\":\"value\"},\"seen\":false,\"seenDate\":1,\"chat\":false,\"topic\":\"topic\"}]}", inboxJSON.toString());
    }

    @Test
    public void inboxMapper_fromBackend_should_include_topic_and_seen() {
        MessageResponse messageResponse = new MessageResponse(
                "message-id",
                "title",
                "message text",
                null,
                "true",
                null,
                null,
                "{\"targetUrl\":\"www.someDomain.com\",\"someData\":\"someData\",\"deeplink\":\"screen_one\"}",
                "{\n" +
                        "  \"bulkId\" : \"some-valid-bulk-id\",\n" +
                        "  \"inApp\" : false,\n" +
                        "  \"atts\" : [ {\n" +
                        "    \"url\" : \"https://www.infobip.com/kittens.png\"\n" +
                        "  } ],\n" +
                        "  \"deeplink\" : \"showcaseApp://deeplink/ProfileScreen\",\n" +
                        "  \"validUntil\" : 1757493217000,\n" +
                        "  \"inbox\" : {\n" +
                        "    \"topic\" : \"Promotions\",\n" +
                        "    \"seen\" : true\n" +
                        "  },\n" +
                        "  \"sendDateTime\" : 1757493207500\n" +
                        "}");
        FetchInboxResponse fetchInboxResponse = new FetchInboxResponse();
        fetchInboxResponse.setCountUnread(4);
        fetchInboxResponse.setCountTotal(5);
        fetchInboxResponse.setCountUnreadFiltered(1);
        fetchInboxResponse.setCountTotalFiltered(1);
        fetchInboxResponse.setMessages(Collections.singletonList(messageResponse));

        Inbox inbox = InboxMapper.fromBackend(fetchInboxResponse);
        InboxMessage inboxMessage = inbox.getMessages().getFirst();

        assertEquals(4, inbox.getCountUnread());
        assertEquals(5, inbox.getCountTotal());
        assertEquals(1, (int) inbox.getCountUnreadFiltered());
        assertEquals(1, (int) inbox.getCountTotalFiltered());
        assertEquals("Promotions", inboxMessage.getTopic());
        assertTrue(inboxMessage.isSeen());
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
