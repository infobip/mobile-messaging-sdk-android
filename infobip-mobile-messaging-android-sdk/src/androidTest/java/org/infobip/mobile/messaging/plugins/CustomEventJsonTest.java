package org.infobip.mobile.messaging.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.CustomEvent;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Objects;

public class CustomEventJsonTest {
    @Test
    public void fromJSON_should_be_ok() throws JSONException {
        String definitionId = "someDefinitionID";
        String someString = "SomeString";
        double someNumber = 12345.0;
        JSONObject jsonObject = new JSONObject("{\"definitionId\":\"" + definitionId + "\",\"properties\":{\"someString\":\"" + someString + "\",\"someNumber\":" + someNumber + ",\"someBool\":true,\"someDate\":\"2024-11-08\"}}");

        CustomEvent customEvent = CustomEventJson.fromJSON(jsonObject);

        assertEquals(definitionId, customEvent.getDefinitionId());
        assertTrue(Objects.requireNonNull(customEvent.getProperties().get("someBool")).booleanValue());
        assertEquals(someString, Objects.requireNonNull(customEvent.getProperties().get("someString")).stringValue());
        assertEquals(someNumber, Objects.requireNonNull(customEvent.getProperties().get("someNumber")).numberValue());
    }
}
