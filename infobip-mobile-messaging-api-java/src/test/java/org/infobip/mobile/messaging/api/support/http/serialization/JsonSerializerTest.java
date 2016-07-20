package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.internal.LinkedTreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class JsonSerializerTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    String getMessageForClassMismatch(Class expeceted, Class observed) {
        return "Expected <" + expeceted.toString() + "> found <" + observed.toString() + ">";
    }

    @Test
    public void execute_JSON_to_Map() throws Exception {
        String json =
                "{" +
                    "\"string\":\"String\"," +
                    "\"boolean\":true," +
                    "\"double\":1.0" +
                "}";

        Map<?,?> jsonMap = new JsonSerializer().deserialize(json, Map.class);
        Map<String, Object> map = new LinkedTreeMap<>();
        Assert.assertEquals(map.getClass(), jsonMap.getClass());

        map = (Map<String, Object>)jsonMap;
        Assert.assertTrue(getMessageForClassMismatch(String.class, map.get("string").getClass()), map.get("string") instanceof String);
        Assert.assertTrue(getMessageForClassMismatch(Boolean.class, map.get("boolean").getClass()), map.get("boolean") instanceof Boolean);
        Assert.assertTrue(getMessageForClassMismatch(Double.class, map.get("double").getClass()), map.get("double") instanceof Double);
    }
}
