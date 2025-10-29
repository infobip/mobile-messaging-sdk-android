/*
 * MessageBundleMapperTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.Message;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author sslavin
 * @since 13/09/2017.
 */

public class MessageBundleMapperTest extends TestCase {

    public void test_shouldSerializeAndDeserializeCustomPayloadWithNestedObjects() throws Exception {

        // Given (some crazy json)
        Message givenMessage = new Message();
        givenMessage.setCustomPayload(new JSONObject()
                .put("level1_1", new JSONObject()
                        .put("level2_1", new JSONObject()
                                .put("level3", "someData"))
                        .put("level2_2", new JSONObject()
                                .put("level3", "someData")))
                .put("level1_2", new JSONObject()
                        .put("level2", new JSONObject()
                                .put("level3", "someData")))
                .put("level1_3", new JSONArray()
                        .put(new JSONArray()
                            .put(new JSONObject()
                                    .put("level3", "someData")))));

        // When
        Message actualMessage = MessageBundleMapper.messageFromBundle(
                MessageBundleMapper.messageToBundle(givenMessage));

        // Then
        JSONAssert.assertEquals(givenMessage.getCustomPayload(), actualMessage.getCustomPayload(), true);
    }
}
