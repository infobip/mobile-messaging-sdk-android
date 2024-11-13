package org.infobip.mobile.messaging.plugins;

import static org.junit.Assert.assertEquals;

import org.infobip.mobile.messaging.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class MessageJsonTest {
    private final static int MESSAGE_JSON_NUMBER_OF_PARAMS = 19;

    @Test
    public void toJSON_should_be_expected_number_of_parameters() throws JSONException {
        Message message = message();

        JSONObject json = MessageJson.toJSON(message);

        assert json != null;
        assertEquals(message.getMessageId(), json.getString("messageId"));
        assertEquals(MESSAGE_JSON_NUMBER_OF_PARAMS, json.length());
    }

    private Message message() {
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
                null,
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
