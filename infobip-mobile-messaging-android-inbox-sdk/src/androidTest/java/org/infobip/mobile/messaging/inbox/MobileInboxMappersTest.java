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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.api.inbox.FetchInboxResponse;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

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
        FetchInboxResponse fetchInboxResponse = new FetchInboxResponse(2, 1, null, null, Collections.singletonList(messageResponse()));
        Inbox inbox = InboxMapper.fromBackend(fetchInboxResponse);

        JSONObject inboxJSON = InboxMapper.toJSON(inbox);
        JSONObject inboxMessage = inboxJSON.getJSONArray("messages").getJSONObject(0);

        //countUnread, countTotal, messages
        assertEquals(3, inboxJSON.length());
        assertEquals(2, inboxJSON.getInt("countTotal"));
        assertEquals(1, inboxJSON.getInt("countUnread"));
        assertFalse(inboxJSON.has("countUnreadFiltered"));
        assertFalse(inboxJSON.has("countTotalFiltered"));
        assertEquals(1, inboxJSON.getJSONArray("messages").length());

        assertEquals(14, inboxMessage.length());
        assertEquals("message-id", inboxMessage.get("messageId"));
        assertEquals("Promotions", inboxMessage.get("topic"));
        assertEquals(true, inboxMessage.get("seen"));
        assertEquals("title-1", inboxMessage.get("title"));
        assertEquals("message text", inboxMessage.get("body"));
        assertEquals("kittens", inboxMessage.get("sound"));
        assertTrue(inboxMessage.getBoolean("vibrate"));
        assertTrue(inboxMessage.getBoolean("silent"));
        assertEquals("{\"targetUrl\":\"www.someDomain.com\",\"someData\":\"someData\",\"deeplink\":\"screen_one\"}", inboxMessage.get("customPayload").toString());
        assertEquals("{\n" +
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
                "}", inboxMessage.get("internalData").toString());
        assertEquals("https://www.infobip.com/kittens.png", inboxMessage.get("contentUrl"));
        assertEquals("showcaseApp://deeplink/ProfileScreen", inboxMessage.get("deeplink"));
        assertEquals(1757493207500L, inboxMessage.get("sentTimestamp"));
    }

    @Test
    public void inboxMapper_fromBackend_should_include_topic_and_seen() {
        MessageResponse messageResponse = messageResponse();
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
        assertEquals(0, inboxMessage.getSeenTimestamp());
        assertEquals(1757493207500L, inboxMessage.getSentTimestamp());
        assertNotEquals(1757493207500L, inboxMessage.getReceivedTimestamp());
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

    private MessageResponse messageResponse() {
        return new MessageResponse(
                "message-id",
                "title-1",
                "message text",
                "kittens",
                "true",
                "true",
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
    }
}
