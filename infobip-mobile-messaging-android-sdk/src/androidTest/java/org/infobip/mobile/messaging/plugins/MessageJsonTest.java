/*
 * MessageJsonTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.plugins;

import org.infobip.mobile.messaging.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageJsonTest {
    private final static int MESSAGE_JSON_NUMBER_OF_PARAMS = 20;

    @Test
    public void toJSON_should_be_expected_number_of_parameters() throws JSONException {
        Message message = message();

        JSONObject json = MessageJson.toJSON(message);

        assert json != null;
        assertEquals(message.getMessageId(), json.getString("messageId"));
        assertEquals(MESSAGE_JSON_NUMBER_OF_PARAMS, json.length());

        JSONObject customs = (JSONObject) json.get("customPayload");
        JSONObject cvstm = message.getCustomPayload();
        assertEquals(cvstm, customs);
    }

    private Message message() throws JSONException {
        return new Message(
                "someID",
                "someTitle",
                "someBody",
                "someSound",
                true,
                "someIcon",
                false,
                "someCat",
                "someFrom",
                0,
                0,
                0,
                new JSONObject("{\"key\": \"value\"}"),
                null,
                "someDestination",
                Message.Status.SUCCESS,
                "statusMessage",
                "http://www.infobip.com",
                Message.InAppStyle.BANNER,
                0,
                "some-web-view-url",
                "some-browser-url",
                "type",
                "deeplink",
                "openTitle",
                "dismissTitle"
        );
    }
}
