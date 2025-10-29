/*
 * JsonSerializerTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.serialization;

import com.google.gson.internal.LinkedTreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class JsonSerializerTest {

    @Test
    public void execute_JSON_to_Map() throws Exception {
        String json =
                "{" +
                        "\"string\":\"String\"," +
                        "\"boolean\":true," +
                        "\"double\":1.0" +
                        "}";

        Map<?, ?> jsonMap = new JsonSerializer().deserialize(json, Map.class);
        Map<String, Object> map = new LinkedTreeMap<>();
        assertEquals(map.getClass(), jsonMap.getClass());

        map = (Map<String, Object>) jsonMap;
        Assert.assertTrue(getMessageForClassMismatch(String.class, map.get("string").getClass()), map.get("string") instanceof String);
        Assert.assertTrue(getMessageForClassMismatch(Boolean.class, map.get("boolean").getClass()), map.get("boolean") instanceof Boolean);
        Assert.assertTrue(getMessageForClassMismatch(Double.class, map.get("double").getClass()), map.get("double") instanceof Double);
    }

    @Test
    public void should_support_custom_types() throws Exception {

        JsonSerializer givenSerializer = new JsonSerializer(false, new JsonSerializer.ObjectAdapter<GivenClass>() {
            @Override
            public Class<GivenClass> getCls() {
                return GivenClass.class;
            }

            @Override
            public GivenClass deserialize(String value) {
                GivenClass obj = new GivenClass();
                obj.string = value.split(":")[1].split("}")[0].split("\"")[1];
                return obj;
            }

            @Override
            public String serialize(GivenClass value) {
                return "{\"strrr\":\"" + value.string + "\"}";
            }
        });
        GivenClass givenObject = new GivenClass();
        givenObject.string = "someValue";

        // When
        String actualSerialized = givenSerializer.serialize(givenObject);
        GivenClass actualDeserialized = givenSerializer.deserialize(actualSerialized, GivenClass.class);

        // Then
        JSONAssert.assertEquals("{\"strrr\":\"someValue\"}", actualSerialized, true);
        assertEquals(givenObject.string, actualDeserialized.string);
    }

    @Test
    public void default_constructor_mapper_should_not_serialize_nulls() throws Exception {
        JsonSerializer givenSerializer = new JsonSerializer();

        Map<String, String> map = new HashMap<>();
        map.put("KITTENS", null);

        String actualSerialized = givenSerializer.serialize(map);
        JSONAssert.assertEquals("{}", actualSerialized, true);
    }

    @Test
    public void parametrized_constructor_mapper_should_serialize_nulls_for_serialize_true() throws Exception {
        JsonSerializer givenSerializer = new JsonSerializer(true);

        Map<String, String> map = new HashMap<>();
        map.put("KITTENS", null);

        String actualSerialized = givenSerializer.serialize(map);
        JSONAssert.assertEquals("{\"KITTENS\":null}", actualSerialized, true);
    }

    @Test
    public void parametrized_constructor_mapper_should_not_serialize_nulls_for_serialize_false() throws Exception {
        JsonSerializer givenSerializer = new JsonSerializer(false);

        Map<String, String> map = new HashMap<>();
        map.put("KITTENS", null);

        String actualSerialized = givenSerializer.serialize(map);
        JSONAssert.assertEquals("{}", actualSerialized, true);
    }

    private String getMessageForClassMismatch(Class expected, Class observed) {
        return "Expected <" + expected.toString() + "> found <" + observed.toString() + ">";
    }

    // Given
    public static class GivenClass {
        String string;
    }
}
