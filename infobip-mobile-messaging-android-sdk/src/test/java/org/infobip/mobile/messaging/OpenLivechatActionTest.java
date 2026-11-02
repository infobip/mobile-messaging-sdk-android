/*
 * OpenLivechatActionTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OpenLivechatActionTest {

    @Test
    public void test_actionFromMissingMessage() {
        assertNull(OpenLivechatAction.parseFrom(null));
    }

    @Test
    public void test_actionFromMissingInternalData() {
        Message message = new Message();
        message.setInternalData(null);

        assertNull(OpenLivechatAction.parseFrom(message));
    }

    @Test
    public void test_actionWithKeyword() {
        Message message = new Message();
        message.setInternalData("{\"openLiveChat\":{\"keyword\":\"testKeyword\"}}");

        assertEquals("testKeyword", OpenLivechatAction.parseFrom(message).keyword);
    }

    @Test
    public void test_actionWithMissingKeyword() {
        Message message = new Message();
        message.setInternalData("{\"openLiveChat\":{\"keyword\":null}}");

        OpenLivechatAction action = OpenLivechatAction.parseFrom(message);
        assertNotNull(action);
        assertNull(action.keyword);
    }

    @Test
    public void test_actionFromChatMessageWithThreadId() throws Exception {
        Message message = new Message();
        message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        message.setCustomPayload(new JSONObject().put("threadId", "testThreadId"));

        OpenLivechatAction action = OpenLivechatAction.parseFrom(message);

        assertNotNull(action);
        assertNull(action.keyword);
        assertEquals("testThreadId", action.threadId);
    }

    @Test
    public void test_actionFromNonChatMessageWithThreadId() throws Exception {
        Message message = new Message();
        message.setCustomPayload(new JSONObject().put("threadId", "testThreadId"));

        assertNull(OpenLivechatAction.parseFrom(message));
    }

    @Test
    public void test_actionFromChatMessageWithMissingThreadId() {
        Message message = new Message();
        message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        message.setCustomPayload(new JSONObject());

        assertNull(OpenLivechatAction.parseFrom(message));
    }

    @Test
    public void test_actionFromChatMessageWithBlankThreadId() throws Exception {
        Message message = new Message();
        message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        message.setCustomPayload(new JSONObject().put("threadId", " "));

        assertNull(OpenLivechatAction.parseFrom(message));
    }

    @Test
    public void test_actionFromChatMessageWithNullThreadId() throws Exception {
        Message message = new Message();
        message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        message.setCustomPayload(new JSONObject().put("threadId", JSONObject.NULL));

        assertNull(OpenLivechatAction.parseFrom(message));
    }

    @Test
    public void test_actionFromBroadcastAndChatMessage() throws Exception {
        Message message = new Message();
        message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        message.setInternalData("{\"openLiveChat\":{\"keyword\":\"testKeyword\"}}");
        message.setCustomPayload(new JSONObject().put("threadId", "testThreadId"));

        OpenLivechatAction action = OpenLivechatAction.parseFrom(message);

        assertNotNull(action);
        assertEquals("testKeyword", action.keyword);
        assertEquals("testThreadId", action.threadId);
    }

}
